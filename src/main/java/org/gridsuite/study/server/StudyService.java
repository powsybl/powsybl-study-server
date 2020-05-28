/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.study.server;

import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.network.store.client.NetworkStoreService;
import org.gridsuite.study.server.dto.NetworkInfos;
import org.gridsuite.study.server.dto.StudyInfos;
import org.gridsuite.study.server.dto.VoltageLevelAttributes;
import org.gridsuite.study.server.repository.Study;
import org.gridsuite.study.server.repository.StudyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.*;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

import static org.gridsuite.study.server.StudyConstants.*;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */

@ComponentScan(basePackageClasses = {NetworkStoreService.class, StudyRepository.class})
@Service
public class StudyService {
    private WebClient webClient;

    String caseServerBaseUri;
    String singleLineDiagramServerBaseUri;
    String networkConversionServerBaseUri;
    String geoDataServerBaseUri;
    String networkMapServerBaseUri;
    String networkModificationServerBaseUri;
    String loadFlowServerBaseUri;

    private final NetworkStoreService networkStoreClient;

    private final StudyRepository studyRepository;

    @Autowired
    public StudyService(
            @Value("${backing-services.case.base-uri:http://case-server/}") String caseServerBaseUri,
            @Value("${backing-services.single-line-diagram.base-uri:http://single-line-diagram-server/}") String singleLineDiagramServerBaseUri,
            @Value("${backing-services.network-conversion.base-uri:http://network-conversion-server/}") String networkConversionServerBaseUri,
            @Value("${backing-services.geo-data.base-uri:http://geo-data-store-server/}") String geoDataServerBaseUri,
            @Value("${backing-services.network-map.base-uri:http://network-map-store-server/}") String networkMapServerBaseUri,
            @Value("${backing-services.network-modification.base-uri:http://network-modification-server/}") String networkModificationServerBaseUri,
            @Value("${backing-services.loadflow.base-uri:http://loadflow-server/}") String loadFlowServerBaseUri,
            NetworkStoreService networkStoreClient,
            StudyRepository studyRepository) {
        this.caseServerBaseUri = caseServerBaseUri;
        this.singleLineDiagramServerBaseUri = singleLineDiagramServerBaseUri;
        this.networkConversionServerBaseUri = networkConversionServerBaseUri;
        this.geoDataServerBaseUri = geoDataServerBaseUri;
        this.networkMapServerBaseUri = networkMapServerBaseUri;
        this.networkModificationServerBaseUri = networkModificationServerBaseUri;
        this.loadFlowServerBaseUri = loadFlowServerBaseUri;

        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024 * 10)).build();
        this.webClient =  WebClient.builder().exchangeStrategies(exchangeStrategies).build();

        this.networkStoreClient = networkStoreClient;
        this.studyRepository = studyRepository;
    }

    Flux<StudyInfos> getStudyList() {
        Flux<Study> studyList = studyRepository.findAll();
        return studyList.map(study -> new StudyInfos(study.getName(), study.getDescription(), study.getCaseFormat()));
    }

    Mono<Study> createStudy(String studyName, UUID caseUuid, String description) {
        Mono<NetworkInfos> networkInfos = persistentStore(caseUuid);
        Mono<String> caseFormat = getCaseFormat(caseUuid);

        return Mono.zip(networkInfos, caseFormat)
                .flatMap(t -> {
                    Study study = new Study(studyName, t.getT1().getNetworkUuid(), t.getT1().getNetworkId(), description, t.getT2(), caseUuid, false);
                    return studyRepository.insert(study);
                });
    }

    private Mono<String> getCaseFormat(UUID caseUuid) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + CASE_API_VERSION + "/cases/{caseUuid}/format")
                .buildAndExpand(caseUuid)
                .toUriString();

        return webClient.get()
                .uri(caseServerBaseUri + path)
                .retrieve()
                .bodyToMono(String.class);
    }

    Mono<Study> createStudy(String studyName, Mono<FilePart> caseFile, String description) {
        Mono<UUID> caseUUid;
        caseUUid = importCase(caseFile);
        return caseUUid.flatMap(uuid -> {
            Mono<NetworkInfos> networkInfos = persistentStore(uuid);
            Mono<String> caseFormat = getCaseFormat(uuid);
            return Mono.zip(networkInfos, caseFormat)
                    .flatMap(t -> {
                        Study study = new Study(studyName, t.getT1().getNetworkUuid(), t.getT1().getNetworkId(), description, t.getT2(), uuid, true);
                        return studyRepository.insert(study);
                    });
        });
    }

    Mono<Study> getStudy(String studyName) {
        return studyRepository.findByName(studyName);
    }

    Mono<Void> deleteStudy(String studyName) {
        Mono<Study> studyMono = studyRepository.findByName(studyName);

        return studyMono.flatMap(study -> {
            String path = UriComponentsBuilder.fromPath(DELIMITER + CASE_API_VERSION + "/cases/{caseUuid}")
                    .buildAndExpand(study.getCaseUuid())
                    .toUriString();
            if (study.isCasePrivate()) {
                return webClient.delete()
                        .uri(caseServerBaseUri + path)
                        .retrieve()
                        .bodyToMono(Void.class)
                        .then(studyRepository.deleteByName(studyName));
            }
            return Mono.empty();
        }).then(studyRepository.deleteByName(studyName));
    }

    Mono<UUID> importCase(Mono<FilePart> multipartFile) {
        return multipartFile.flatMap(file -> {
            MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
            multipartBodyBuilder.part("file", file);

            return webClient.post()
                    .uri(caseServerBaseUri + "/" + CASE_API_VERSION + "/cases/private")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA.toString())
                    .body(BodyInserters.fromMultipartData(multipartBodyBuilder.build()))
                    .retrieve()
                    .bodyToMono(UUID.class);
        });
    }

    Mono<byte[]> getVoltageLevelSvg(UUID networkUuid, String voltageLevelId, boolean useName, boolean centerLabel, boolean diagonalLabel,
                                    boolean topologicalColoring) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + SINGLE_LINE_DIAGRAM_API_VERSION + "/svg/{networkUuid}/{voltageLevelId}")
                .queryParam("useName", useName)
                .queryParam("centerLabel", centerLabel)
                .queryParam("diagonalLabel", diagonalLabel)
                .queryParam("topologicalColoring", topologicalColoring)
                .buildAndExpand(networkUuid, voltageLevelId)
                .toUriString();

        return webClient.get()
                .uri(singleLineDiagramServerBaseUri + path)
                .retrieve()
                .bodyToMono(byte[].class);
    }

    Mono<String> getVoltageLevelSvgAndMetadata(UUID networkUuid, String voltageLevelId, boolean useName, boolean centerLabel, boolean diagonalLabel,
                                               boolean topologicalColoring) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + SINGLE_LINE_DIAGRAM_API_VERSION + "/svg-and-metadata/{networkUuid}/{voltageLevelId}")
                .queryParam("useName", useName)
                .queryParam("centerLabel", centerLabel)
                .queryParam("diagonalLabel", diagonalLabel)
                .queryParam("topologicalColoring", topologicalColoring)
                .buildAndExpand(networkUuid, voltageLevelId)
                .toUriString();

        return webClient.get()
                .uri(singleLineDiagramServerBaseUri + path)
                .retrieve()
                .bodyToMono(String.class);
    }

    private Mono<NetworkInfos> persistentStore(UUID caseUuid) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + NETWORK_CONVERSION_API_VERSION + "/networks")
                .queryParam(CASE_UUID, caseUuid)
                .buildAndExpand()
                .toUriString();

        return webClient.post()
                .uri(networkConversionServerBaseUri + path)
                .retrieve()
                .bodyToMono(NetworkInfos.class);
    }

    // TO CONVERT TO NON BLOCKING CALL
    List<VoltageLevelAttributes> getNetworkVoltageLevels(UUID networkUuid) {
        ArrayList<VoltageLevelAttributes> voltageLevelAttributes = new ArrayList<>();
        Iterable<VoltageLevel> voltageLevels = networkStoreClient.getNetwork(networkUuid).getVoltageLevels();
        for (VoltageLevel voltageLevel : voltageLevels) {
            String voltageLevelId = voltageLevel.getId();
            String voltageName = voltageLevel.getName();
            String substationId = voltageLevel.getSubstation().getId();
            voltageLevelAttributes.add(new VoltageLevelAttributes(voltageLevelId, voltageName, substationId));
        }
        return voltageLevelAttributes;
    }

    Mono<String> getLinesGraphics(UUID networkUuid) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + GEO_DATA_API_VERSION + "/lines")
                .queryParam(NETWORK_UUID, networkUuid)
                .buildAndExpand()
                .toUriString();

        return webClient.get()
                .uri(geoDataServerBaseUri + path)
                .retrieve()
                .bodyToMono(String.class);
    }

    Mono<String> getSubstationsGraphics(UUID networkUuid) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + GEO_DATA_API_VERSION + "/substations")
                .queryParam(NETWORK_UUID, networkUuid)
                .buildAndExpand()
                .toUriString();

        return webClient.get()
                .uri(geoDataServerBaseUri + path)
                .retrieve()
                .bodyToMono(String.class);
    }

    Mono<Boolean> caseExists(UUID caseUuid) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + CASE_API_VERSION + "/cases/{caseUuid}/exists")
                .buildAndExpand(caseUuid)
                .toUriString();

        return webClient.get()
                .uri(caseServerBaseUri + path)
                .retrieve()
                .bodyToMono(Boolean.class);
    }

    Mono<String> getSubstationsMapData(UUID networkUuid) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + CASE_API_VERSION + "/substations/{networkUuid}")
                .buildAndExpand(networkUuid)
                .toUriString();

        return webClient.get()
                .uri(networkMapServerBaseUri + path)
                .retrieve()
                .bodyToMono(String.class);
    }

    Mono<String> getLinesMapData(UUID networkUuid) {
        String path = UriComponentsBuilder.fromPath(DELIMITER + CASE_API_VERSION + "/lines/{networkUuid}")
                .buildAndExpand(networkUuid)
                .toUriString();

        return webClient.get()
                .uri(networkMapServerBaseUri + path)
                .retrieve()
                .bodyToMono(String.class);
    }

    Mono<Void> changeSwitchState(String studyName, String switchId, boolean open) {
        Mono<UUID> networkUuid = getStudyUuid(studyName);

        return networkUuid.flatMap(uuid -> {
            String path = UriComponentsBuilder.fromPath(DELIMITER + NETWORK_MODIFICATION_API_VERSION + "/networks/{networkUuid}/switches/{switchId}")
                    .queryParam("open", open)
                    .buildAndExpand(uuid, switchId)
                    .toUriString();
            return webClient.put()
                    .uri(networkModificationServerBaseUri + path)
                    .retrieve()
                    .bodyToMono(Void.class);
        });
    }

    Mono<Void> runLoadFlow(String studyName) {
        Mono<UUID> networkUuid = getStudyUuid(studyName);

        return networkUuid.flatMap(uuid -> {
            String path = UriComponentsBuilder.fromPath(DELIMITER + LOADFLOW_API_VERSION + "/networks/{networkUuid}/run")
                    .buildAndExpand(uuid)
                    .toUriString();

            return webClient.put()
                    .uri(loadFlowServerBaseUri + path)
                    .retrieve()
                    .bodyToMono(Void.class);
        });
    }

    @Transactional
    public Mono<Study> renameStudy(String studyName, String newStudyName) {
        Mono<Study> studyMono = studyRepository.findByName(studyName);
        return studyMono.switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, STUDY_DOESNT_EXISTS))).flatMap(study -> {
            study.setName(newStudyName);
            Mono<Study> newStudyMono = studyRepository.insert(study);
            Mono<Void> deleted = studyRepository.deleteByName(studyName);
            return deleted.then(newStudyMono);
        });
    }

    Mono<UUID> getStudyUuid(String studyName) {
        Mono<Study> studyMono = studyRepository.findByName(studyName);
        return studyMono.map(Study::getNetworkUuid)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, STUDY_DOESNT_EXISTS)));

    }

    Mono<Boolean> studyExists(String studyName) {
        return getStudy(studyName).hasElement();
    }

    public Mono<Void> assertCaseExists(UUID caseUuid) {
        Mono<Boolean> caseExists = caseExists(caseUuid);
        return caseExists.flatMap(c -> (boolean) c ? Mono.empty() : Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, CASE_DOESNT_EXISTS)));
    }

    public Mono<Void> assertStudyNotExists(String studyName) {
        Mono<Boolean> studyExists = studyExists(studyName);
        return studyExists.flatMap(s -> (boolean) s ? Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, STUDY_ALREADY_EXISTS)) : Mono.empty());
    }

    void setCaseServerBaseUri(String caseServerBaseUri) {
        this.caseServerBaseUri = caseServerBaseUri;
    }

    void setNetworkConversionServerBaseUri(String networkConversionServerBaseUri) {
        this.networkConversionServerBaseUri = networkConversionServerBaseUri;
    }

    void setGeoDataServerBaseUri(String geoDataServerBaseUri) {
        this.geoDataServerBaseUri = geoDataServerBaseUri;
    }

    void setSingleLineDiagramServerBaseUri(String singleLineDiagramServerBaseUri) {
        this.singleLineDiagramServerBaseUri = singleLineDiagramServerBaseUri;
    }

    void setNetworkModificationServerBaseUri(String networkModificationServerBaseUri) {
        this.networkModificationServerBaseUri = networkModificationServerBaseUri;
    }

    void setNetworkMapServerBaseUri(String networkMapServerBaseUri) {
        this.networkMapServerBaseUri = networkMapServerBaseUri;
    }

    void setLoadFlowServerBaseUri(String loadFlowServerBaseUri) {
        this.loadFlowServerBaseUri = loadFlowServerBaseUri;
    }
}
