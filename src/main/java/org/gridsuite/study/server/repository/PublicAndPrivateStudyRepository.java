/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.study.server.repository;

import org.gridsuite.study.server.dto.LoadFlowStatus;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.cassandra.repository.ReactiveCassandraRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */

@Repository
public interface PublicAndPrivateStudyRepository extends ReactiveCassandraRepository<PublicAndPrivateStudyEntity, Integer> {

    Mono<PublicAndPrivateStudyEntity> findByUserIdAndStudyName(String userId, String name);

    @Query("DELETE FROM study WHERE userId = :userId and studyname = :studyName")
    Mono<Void> deleteByStudyNameAndUserId(@Param("studyName") String studyName, @Param("userId") String userId);

    @Query("UPDATE study SET loadFlowResult.status = :status WHERE userId = :userId and studyname = :studyName IF EXISTS")
    Mono<Object> updateLoadFlowState(String studyName, String userId, LoadFlowStatus status);

    @Query("UPDATE study SET loadFlowResult = :result WHERE userId = :userId and studyname = :studyName IF EXISTS")
    Mono<Object> updateLoadFlowResult(String studyName, String userId, LoadFlowResultEntity result);

    @Query("UPDATE study SET loadFlowParameters = :lfParameter  WHERE userId = :userId and studyname = :studyName IF EXISTS")
    Mono<Object> updateLoadFlowParameters(String studyName, String userId, LoadFlowParametersEntity lfParameter);

}
