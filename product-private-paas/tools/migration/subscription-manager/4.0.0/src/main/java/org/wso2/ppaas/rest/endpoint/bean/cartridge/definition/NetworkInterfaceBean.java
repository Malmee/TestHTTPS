/*
 * Copyright (c) 2005-2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.ppaas.rest.endpoint.bean.cartridge.definition;

/**
 * @author Jeffrey Nguyen
 *
 */
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement (name = "networkInterfaces")
public class NetworkInterfaceBean {
	public String networkUuid;
	public String fixedIp;
	public String portUuid;
    public String toString () {
    	StringBuilder sb = new StringBuilder('{');
    	String delimeter = "";
    	if (networkUuid != null) {
    		sb.append(delimeter).append("networkUuid : ").append(networkUuid);
    		delimeter = ", ";
    	}
    	if (fixedIp != null) {
    		sb.append(delimeter).append("fixedIp : ").append(fixedIp);
    		delimeter = ", ";
    	}
    	if (portUuid != null) {
    		sb.append(delimeter).append("portUuid : ").append(portUuid);
    		delimeter = ", ";
    	}
    	sb.append('}');
        return sb.toString();
    }
}
