/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.study.server.repository;

import org.gridsuite.study.server.dto.LoadFlowResult;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.cassandra.repository.ReactiveCassandraRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */

@Repository
public interface PrivateStudyRepository extends ReactiveCassandraRepository<PrivateStudy, Integer> {

    Flux<PrivateStudy> findAllByUserId(String userId);

    @Query("DELETE FROM privateStudy WHERE userId = :userId and studyname = :studyName")
    Mono<Void> delete(@Param("userId") String userId, @Param("studyName") String studyName);

    @Query("UPDATE privateStudy SET loadFlowResult.status = :status WHERE userId = :userId and studyname = :studyName IF isPrivate != null")
    Mono<Void> updateLoadFlowState(String studyName, String userId, LoadFlowResult.LoadFlowStatus status);

    @Query("UPDATE privateStudy SET loadFlowResult = :result WHERE userId = :userId and studyname = :studyName IF isPrivate != null")
    Mono<Void> updateLoadFlowResult(String studyName, String userId, LoadFlowResult result);

}
