/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.study.server.repositories;

import org.gridsuite.study.server.entities.ComponentResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */

public interface ComponentResultRepository extends JpaRepository<ComponentResultEntity, Long> {
}
