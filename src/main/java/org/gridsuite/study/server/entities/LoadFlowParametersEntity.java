/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.study.server.entities;

import com.powsybl.loadflow.LoadFlowParameters;
import lombok.*;

import javax.persistence.*;
import java.io.Serializable;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 * @author Jacques Borsenberger <Jacques.Borsenberger at rte-france.com>
 */

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "loadFlowParameters")
public class LoadFlowParametersEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private long id;

    @Column(name = "voltageInitMode")
    private LoadFlowParameters.VoltageInitMode voltageInitMode;

    @Column(name = "transformerVoltageControlOn")
    private boolean transformerVoltageControlOn;

    @Column(name = "noGeneratorReactiveLimits")
    private boolean noGeneratorReactiveLimits;

    @Column(name = "phaseShifterRegulationOn")
    private boolean phaseShifterRegulationOn;

    @Column(name = "twtSplitShuntAdmittance")
    private boolean twtSplitShuntAdmittance;

    @Column(name = "simulShunt")
    private boolean simulShunt;

    @Column(name = "readSlackBus")
    private boolean readSlackBus;

    @Column(name = "writeSlackBus")
    private boolean writeSlackBus;

    @Column(name = "dc")
    private boolean dc;

    @Column(name = "distributedSlack")
    private boolean distributedSlack;

    @Column(name = "balanceType")
    private LoadFlowParameters.BalanceType balanceType;
}
