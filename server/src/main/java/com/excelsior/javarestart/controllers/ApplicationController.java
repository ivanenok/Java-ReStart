/*
 * Copyright (c) 2013-2014, Nikita Lipsky, Excelsior LLC.
 *
 *  Java ReStart is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Java ReStart is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 *
*/
package com.excelsior.javarestart.controllers;

import com.excelsior.javarestart.appresourceprovider.AppResourceProvider;
import com.excelsior.javarestart.appresourceprovider.ResourceNotFoundException;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * @author Nikita Lipsky
 */
@Controller
public class ApplicationController {

    @Value("${apps.path}")
    private String rootDir;

    private HashMap<String, AppResourceProvider> projectLoader = new HashMap<String, AppResourceProvider>();

    Logger logger = Logger.getLogger(ApplicationController.class.getName());

    private AppResourceProvider getOrRegisterApp(String applicationName) {
        AppResourceProvider resourceProvider = projectLoader.get(applicationName);
        if (resourceProvider == null) {
            try {
                resourceProvider = new AppResourceProvider(rootDir, applicationName);
                projectLoader.put(applicationName, resourceProvider);
            } catch (Exception e) {
                logger.warning(e.toString());
            }
        }
        return resourceProvider;
    }

    @ResponseBody
    @RequestMapping(value = "/{applicationName}", method = RequestMethod.GET)
    public String getMain(@PathVariable("applicationName") String applicationName, HttpServletResponse response) throws Exception {
        AppResourceProvider resourceProvider = getOrRegisterApp(applicationName);
        if (resourceProvider == null) {
            response.sendError(404);
            return null;
        }
        return resourceProvider.getMain();
    }

	@RequestMapping(value = "/{applicationName}", params ={"resource"} ,method = RequestMethod.GET)
	public void loadResource(@RequestParam(value = "resource") String resourceName, @PathVariable("applicationName") String applicationName, HttpServletResponse response) throws Exception {
        AppResourceProvider resourceProvider = getOrRegisterApp(applicationName);
        InputStream resource = null;
        try {
            resource = resourceProvider.load(resourceName);
            IOUtils.copy(resource, response.getOutputStream());
            response.flushBuffer();
            logger.info("Class or resource loaded: " + resourceName);
        }catch (ResourceNotFoundException e) {
            logger.warning(e.toString());
            response.sendError(404);
        } finally {
            try {
                if (resource != null)
                    resource.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
	}

}