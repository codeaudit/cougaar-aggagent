/* 
 * <copyright>
 *  Copyright 2002-2003 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */
 
package org.cougaar.lib.aggagent.servlet;           

import javax.servlet.*;
import javax.servlet.http.*;

import java.io.*;
import java.util.*;

/** 
 *
 * This is a servlet filter that logs all input and output data.  It has one 
 * optional init parameter "filename" whils specifies the file to write to.
 * If filename is not specified, data is written to stdout.
 */
public class LogFilter implements Filter {
      
    FilterConfig config;
    String outputFileName;
    public void init(javax.servlet.FilterConfig filterConfig) throws javax.servlet.ServletException {
        config = filterConfig;
        outputFileName = filterConfig.getInitParameter("filename");
        filterConfig.getServletContext().log("LogFilter installed: logging to "+((outputFileName == null) ? "stdout" : outputFileName));
    }
    
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws java.io.IOException, javax.servlet.ServletException {

        RequestWrapper reqWrap = new RequestWrapper(servletRequest);
        ResponseWrapper respWrap = new ResponseWrapper(servletResponse);
        
        filterChain.doFilter(reqWrap, respWrap);

        PrintWriter out = servletResponse.getWriter();
        out.write(respWrap.toString());
        out.close();

        PrintWriter logout;
        FileWriter file = null;
        if (outputFileName != null) {
            file = new FileWriter(outputFileName);
            logout = new PrintWriter(file);
        } else {
            logout = new PrintWriter(System.out);
        }
        
        Enumeration params = servletRequest.getParameterNames();
        logout.println("--INPUT--");
        while (params.hasMoreElements()) {
            String param = (String)params.nextElement();
            String value = servletRequest.getParameter(param);
            logout.println("Param:"+param+" -> "+value);
        }

        String input = reqWrap.getInputStreamData();
        if (input != null) {
          logout.println("  Stream Data: ");
          logout.println(reqWrap.getInputStreamData());
        }
        
        logout.println("--OUTPUT--");
        logout.println(respWrap.toString());
        
        logout.flush();
        if (file != null) 
            file.close();

    }
    
    public void destroy() {
    }
    
    class RequestWrapper extends HttpServletRequestWrapper {
        
        private ServletInputStream input = null;
        private ByteArrayOutputStream baos = null;
        
        public RequestWrapper(ServletRequest req) {
            super((HttpServletRequest)req);
        }
        
        public String getInputStreamData() {
            if (baos == null)
                return null;
            
            return new String (baos.toByteArray());
        }
        
        public ServletInputStream getInputStream() throws IOException {
            input =  new LoggingInputStream(getRequest().getInputStream());
            baos = new ByteArrayOutputStream();
            return input;
        }
        
        class LoggingInputStream extends ServletInputStream  {
            ServletInputStream real;
            public LoggingInputStream (ServletInputStream input) {
                real = input;
            }
            
            public int read() throws java.io.IOException {
                int ret = real.read();
                if (ret >= 0)
                    baos.write(ret);
                return ret;
            }   
            public int readLine(byte[] b, int off, int len) throws IOException {
                int ret = real.readLine(b, off, len);
                if (ret >= 0)
                    baos.write(b,off,ret);
                return ret;
            }
            public int read(byte[] b, int off, int len)  throws IOException {
                int ret = real.read(b, off, len);
                if (ret >= 0)
                    baos.write(b,off,ret);
                return ret;
            }
            public int available() throws IOException  {
                return real.available();
            }
            public void close() throws IOException  {
                real.close();
            }
            public void mark(int readlimit)   {
                real.mark(readlimit);
            }
            public boolean markSupported() {
                return real.markSupported();
            }
            public int read(byte [] b)  throws IOException {
                int ret = real.read(b);
                if (ret >= 0)
                    baos.write(b);
                return ret;
            }
            public void reset() throws IOException  {
                real.reset();
            }
            public long skip(long n) throws IOException  {
                return real.skip(n);
            }
            
        }

    }


    private class ResponseWrapper extends HttpServletResponseWrapper {
        public ResponseWrapper(ServletResponse resp) {
            super((HttpServletResponse)resp);
            output = new CharArrayWriter();
        }
        public PrintWriter getWriter() throws IOException {
            return new PrintWriter(output);
        }
        private CharArrayWriter output;
        public String toString() {
            return output.toString();
        }
    }
}
