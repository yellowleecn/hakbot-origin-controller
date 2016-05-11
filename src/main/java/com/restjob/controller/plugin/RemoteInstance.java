/*
 * This file is part of RESTjob Controller.
 *
 * RESTjob Controller is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * RESTjob Controller is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * RESTjob Controller. If not, see http://www.gnu.org/licenses/.
 */
package com.restjob.controller.plugin;

import com.restjob.controller.logging.Logger;
import java.net.MalformedURLException;
import java.net.URL;

public class RemoteInstance {

    // Setup logging
    private static final Logger logger = Logger.getLogger(RemoteInstance.class);

    private String alias;
    private URL url;
    private String username;
    private String password;
    private String apiKey;
    private boolean validateCertificates;

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public URL getURL() {
        return url;
    }

    public void setURL(URL url) {
        this.url = url;
    }

    public String getUrl() {
        return url.toExternalForm();
    }

    public void setUrl(String url) {
        try {
            this.url = new URL(url);
        } catch (MalformedURLException e) {
            logger.error(e.getMessage());
        }
    }

    public boolean isValidateCertificates() {
        return validateCertificates;
    }

    public void setValidateCertificates(boolean validateCertificates) {
        this.validateCertificates = validateCertificates;
    }
}
