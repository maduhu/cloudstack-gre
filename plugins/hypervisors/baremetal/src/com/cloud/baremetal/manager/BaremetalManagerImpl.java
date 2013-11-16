// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
// 
//   http://www.apache.org/licenses/LICENSE-2.0
// 
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
// 
// Automatically generated by addcopyright.py at 01/29/2013
package com.cloud.baremetal.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.AddBaremetalHostCmd;
import org.apache.log4j.Logger;

import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.manager.Commands;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.fsm.StateListener;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineGuru;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VirtualMachineName;
import com.cloud.vm.VirtualMachine.Event;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.VirtualMachineProfile;

@Local(value = {BaremetalManager.class})
public class BaremetalManagerImpl extends ManagerBase implements BaremetalManager,  StateListener<State, VirtualMachine.Event, VirtualMachine> {
	private static final Logger s_logger = Logger.getLogger(BaremetalManagerImpl.class);
	
	@Inject
	protected HostDao _hostDao;
	
	@Override
	public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
		VirtualMachine.State.getStateMachine().registerListener(this);
		return true;
	}

	@Override
	public boolean start() {
		return true;
	}

	@Override
	public boolean stop() {
		return true;
	}

	@Override
	public String getName() {
		return "Baremetal Manager";
	}

	@Override
    public boolean preStateTransitionEvent(State oldState, Event event, State newState, VirtualMachine vo, boolean status, Object opaque) {
	    return false;
    }

	@Override
    public boolean postStateTransitionEvent(State oldState, Event event, State newState, VirtualMachine vo, boolean status, Object opaque) {
		if (newState != State.Starting && newState != State.Error && newState != State.Expunging) {
			return true;
		}
		
		if (vo.getHypervisorType() != HypervisorType.BareMetal) {
		    return true;
		}
		
		HostVO host = _hostDao.findById(vo.getHostId());
		if (host == null) {
			s_logger.debug("Skip oldState " + oldState + " to " + "newState " + newState + " transimtion");
			return true;
		}
		_hostDao.loadDetails(host);
		
		if (newState == State.Starting) {
			host.setDetail("vmName", vo.getInstanceName());
			s_logger.debug("Add vmName " + host.getDetail("vmName") + " to host " + host.getId() + " details");
		} else {
			if (host.getDetail("vmName") != null && host.getDetail("vmName").equalsIgnoreCase(vo.getInstanceName())) {
				s_logger.debug("Remove vmName " + host.getDetail("vmName") + " from host " + host.getId() + " details");
				host.getDetails().remove("vmName");
			}
		}
		_hostDao.saveDetails(host);
		
		
		return true;
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmds = new ArrayList<Class<?>>();
        cmds.add(AddBaremetalHostCmd.class);
        return cmds;
    }
}
