/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.study.server.entities;

import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 * @author Etienne Homer <etienne.homer at rte-france.com>
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "loadFlowResult")
public class LoadFlowResultEntity implements Serializable {

    public LoadFlowResultEntity(boolean ok, Map<String, String> metrics, String logs, List<ComponentResultEntity> componentResults) {
        this.ok = ok;
        this.metrics = metrics;
        this.logs = logs;
        this.componentResults = componentResults;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private long id;

    @Column(name = "ok")
    private boolean ok;

    @Column(name = "metrics")
    @ElementCollection
    private Map<String, String> metrics;

    @Column(name = "logs")
    private String logs;

    @OneToMany(mappedBy = "loadFlowResult")
    private List<ComponentResultEntity> componentResults;

}
