/*
 * Copyright (C) 2016  Hendrik Borghorst & Frederik Luetkes
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.gateshipone.odyssey.artworkdatabase.network.requests;

import com.android.volley.Request;
import com.android.volley.Response;

import org.gateshipone.odyssey.BuildConfig;

import java.util.HashMap;
import java.util.Map;

public abstract class OdysseyRequest<T> extends Request<T>{
    public OdysseyRequest(int method, String url, Response.ErrorListener listener) {
        super(method, url, listener);
    }

    @Override
    public Map<String, String> getHeaders(){
        Map<String, String> headers = new HashMap<>();
        headers.put("User-agent", "Application Odyssey/" + BuildConfig.VERSION_NAME + " (https://github.com/gateship-one/odyssey)");
        return headers;
    }
}
