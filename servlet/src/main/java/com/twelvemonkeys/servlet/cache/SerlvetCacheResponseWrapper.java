/*
 * Copyright (c) 2008, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name "TwelveMonkeys" nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.servlet.cache;

import com.twelvemonkeys.lang.StringUtil;
import com.twelvemonkeys.net.HTTPUtil;
import com.twelvemonkeys.servlet.ServletResponseStreamDelegate;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

/**
 * CacheResponseWrapper class description.
 * <p/>
 * Based on ideas and code found in the ONJava article
 * <a href="http://www.onjava.com/pub/a/onjava/2003/11/19/filters.html">Two
 * Servlet Filters Every Web Application Should Have</a>
 * by Jayson Falkner.
 *
 * @author Jayson Falkner
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: SerlvetCacheResponseWrapper.java#2 $
 */
class SerlvetCacheResponseWrapper extends HttpServletResponseWrapper {
    private ServletResponseStreamDelegate streamDelegate;

    private CacheResponse cacheResponse;

    private Boolean cacheable;
    private int status;

    public SerlvetCacheResponseWrapper(final HttpServletResponse pServletResponse, final CacheResponse pResponse) {
        super(pServletResponse);
        cacheResponse = pResponse;
        init();
    }


    /*
     NOTE: This class defers determining if a response is cacheable until the
     output stream is needed.
     This it the reason for the somewhat complicated logic in the add/setHeader
     methods below.
     */
    private void init() {
        cacheable = null;
        status = SC_OK;
        streamDelegate = new ServletResponseStreamDelegate(this) {
            protected OutputStream createOutputStream() throws IOException {
                // Test if this request is really cacheable, otherwise,
                // just write through to underlying response, and don't cache
                if (isCacheable()) {
                    return cacheResponse.getOutputStream();
                }
                else {
                    // TODO: We need to tell the cache about this, somehow...
                    writeHeaders(cacheResponse, (HttpServletResponse) getResponse());
                    return super.getOutputStream();
                }
            }
        };
    }

    private void writeHeaders(final CacheResponse pResponse, final HttpServletResponse pServletResponse) {
        Map<String,List<String>> headers = pResponse.getHeaders();
        for (Map.Entry<String, List<String>> header : headers.entrySet()) {
            for (int i = 0; i < header.getValue().size(); i++) {
                String value = header.getValue().get(i);
                if (i == 0) {
                    pServletResponse.setHeader(header.getKey(), value);
                }
                else {
                    pServletResponse.addHeader(header.getKey(), value);
                }
            }
        }
    }

    public boolean isCacheable() {
        // NOTE: Intentionally not synchronized
        if (cacheable == null) {
            cacheable = isCacheableImpl();
        }

        return cacheable;
    }

    private boolean isCacheableImpl() {
        // TODO: This code is duped in the cache...
        if (status != SC_OK) {
            return false;
        }

        // Vary: *
        List<String> values = cacheResponse.getHeaders().get(HTTPCache.HEADER_VARY);
        if (values != null) {
            for (String value : values) {
                if ("*".equals(value)) {
                    return false;
                }
            }
        }

        // Cache-Control: no-cache, no-store, must-revalidate
        values = cacheResponse.getHeaders().get(HTTPCache.HEADER_CACHE_CONTROL);
        if (values != null) {
            for (String value : values) {
                if (StringUtil.contains(value, "no-cache")
                        || StringUtil.contains(value, "no-store")
                        || StringUtil.contains(value, "must-revalidate")) {
                    return false;
                }
            }
        }

        // Pragma: no-cache
        values = cacheResponse.getHeaders().get(HTTPCache.HEADER_PRAGMA);
        if (values != null) {
            for (String value : values) {
                if (StringUtil.contains(value, "no-cache")) {
                    return false;
                }
            }
        }

        return true;
    }

    public void flushBuffer() throws IOException {
        streamDelegate.flushBuffer();
    }

    public void resetBuffer() {
        // Servlet 2.3
        streamDelegate.resetBuffer();
    }

    public void reset() {
        if (Boolean.FALSE.equals(cacheable)) {
            super.reset();
        }
        // No else, might be cacheable after all..
        init();
    }

    public ServletOutputStream getOutputStream() throws IOException {
        return streamDelegate.getOutputStream();
    }

    public PrintWriter getWriter() throws IOException {
        return streamDelegate.getWriter();
    }

    public boolean containsHeader(String name) {
        return cacheResponse.getHeaders().get(name) != null;
    }

    public void sendError(int pStatusCode, String msg) throws IOException {
        // NOT cacheable
        status = pStatusCode;
        super.sendError(pStatusCode, msg);
    }

    public void sendError(int pStatusCode) throws IOException {
        // NOT cacheable
        status = pStatusCode;
        super.sendError(pStatusCode);
    }

    public void setStatus(int pStatusCode, String sm) {
        // NOTE: This method is deprecated
        setStatus(pStatusCode);
    }

    public void setStatus(int pStatusCode) {
        // NOT cacheable unless pStatusCode == 200 (or a FEW others?)
        if (pStatusCode != SC_OK) {
            status = pStatusCode;
            super.setStatus(pStatusCode);
        }
    }

    public void sendRedirect(String pLocation) throws IOException {
        // NOT cacheable
        status = SC_MOVED_TEMPORARILY;
        super.sendRedirect(pLocation);
    }

    public void setDateHeader(String pName, long pValue) {
        // If in write-trough-mode, set headers directly
        if (Boolean.FALSE.equals(cacheable)) {
            super.setDateHeader(pName, pValue);
        }
        cacheResponse.setHeader(pName, HTTPUtil.formatHTTPDate(pValue));
    }

    public void addDateHeader(String pName, long pValue) {
        // If in write-trough-mode, set headers directly
        if (Boolean.FALSE.equals(cacheable)) {
            super.addDateHeader(pName, pValue);
        }
        cacheResponse.addHeader(pName, HTTPUtil.formatHTTPDate(pValue));
    }

    public void setHeader(String pName, String pValue) {
        // If in write-trough-mode, set headers directly
        if (Boolean.FALSE.equals(cacheable)) {
            super.setHeader(pName, pValue);
        }
        cacheResponse.setHeader(pName, pValue);
    }

    public void addHeader(String pName, String pValue) {
        // If in write-trough-mode, set headers directly
        if (Boolean.FALSE.equals(cacheable)) {
            super.addHeader(pName, pValue);
        }
        cacheResponse.addHeader(pName, pValue);
    }

    public void setIntHeader(String pName, int pValue) {
        // If in write-trough-mode, set headers directly
        if (Boolean.FALSE.equals(cacheable)) {
            super.setIntHeader(pName, pValue);
        }
        cacheResponse.setHeader(pName, String.valueOf(pValue));
    }

    public void addIntHeader(String pName, int pValue) {
        // If in write-trough-mode, set headers directly
        if (Boolean.FALSE.equals(cacheable)) {
            super.addIntHeader(pName, pValue);
        }
        cacheResponse.addHeader(pName, String.valueOf(pValue));
    }

    public final void setContentType(String type) {
        setHeader(HTTPCache.HEADER_CONTENT_TYPE, type);
    }
}