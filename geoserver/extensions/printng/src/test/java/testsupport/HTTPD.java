package testsupport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * copied from old work and stripped of real functionality
 *
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public abstract class HTTPD {

    boolean stop = false;
    ServerSocket ss;
    ThreadLocal<Handler> handler = new ThreadLocal<Handler>();

    /**
     * Starts a HTTP server to given port.<p> Throws an IOException if the
     * socket is already in use
     */
    public HTTPD() {
        ss = openServer();
        Thread thread = new Thread(new Runnable() {
            public void run() {
                try {
                    while (!stop) {
                        new Handler(ss.accept());
                    }
                } catch (IOException ioe) {
                    // pass
                }
            }
        });
        thread.setPriority(3);
        thread.setDaemon(true);
        thread.start();
    }

    public int getPort() {
        return ss.getLocalPort();
    }

    public void stop() {
        try {
            ss.close();
        } catch (IOException ex) {
            // pass
        }
        stop = true;
    }

    private ServerSocket openServer() {
        for (int i = 10000; i < 20000; i++) {
            try {
                return new ServerSocket(i);
            } catch (IOException ex) {
                // pass
            }
        }
        throw new RuntimeException("Couldn't find open port");
    }

    protected abstract void serve(String uri, String method, Properties header, Properties parms);
    
    protected void sendResponse(String status, String mime, Properties header, String data) {
        handler.get().sendResponse(status, mime, header, data);
    }

    private class Handler implements Runnable {

        SimpleDateFormat gmtFrmt = new SimpleDateFormat("E, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
        Socket socket;

        public Handler(Socket s) {
            socket = s;
            Thread t = new Thread(this);
            t.setDaemon(true);
            t.start();
        }

        public void run() {
            handler.set(this);
            try {
                InputStream is = socket.getInputStream();
                if (is == null) {
                    return;
                }
                BufferedReader in = new BufferedReader(new InputStreamReader(is));

                // Read the request line
                StringTokenizer st = new StringTokenizer(in.readLine());

                String method = st.nextToken();

                String uri = URLDecoder.decode(st.nextToken(), "ASCII");

                // Decode parameters from the URI
                Properties parms = new Properties();
                int qmi = uri.indexOf('?');
                if (qmi >= 0) {
                    decodeParms(uri.substring(qmi + 1), parms);
                    uri = URLDecoder.decode(uri.substring(0, qmi), "ASCII");
                }

                Properties header = new Properties();
                if (st.hasMoreTokens()) {
                    String line = in.readLine();
                    while (line != null && line.trim().length() > 0) {
                        int p = line.indexOf(':');
                        header.put(line.substring(0, p).trim().toLowerCase(), line.substring(p + 1).trim());
                        line = in.readLine();
                    }
                }

                serve(uri, method, header, parms);

                in.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            } catch (InterruptedException ie) {
                // Thrown by sendError, ignore and exit the thread.
            }
        }

        /**
         * Decodes parameters in percent-encoded URI-format ( e.g.
         * "name=Jack%20Daniels&pass=Single%20Malt" ) and adds them to given
         * Properties.
         */
        private void decodeParms(String parms, Properties p)
                throws InterruptedException, UnsupportedEncodingException {
            if (parms == null) {
                return;
            }

            StringTokenizer st = new StringTokenizer(parms, "&");
            while (st.hasMoreTokens()) {
                String e = st.nextToken();
                int sep = e.indexOf('=');
                if (sep >= 0) {
                    p.put(URLDecoder.decode(e.substring(0, sep), "ASCII").trim(),
                            URLDecoder.decode(e.substring(sep + 1), "ASCII"));
                }
            }
        }

        /**
         * Sends given response to the socket.
         */
        protected void sendResponse(String status, String mime, Properties header, String data) {
            try {
                OutputStream out = socket.getOutputStream();
                PrintWriter pw = new PrintWriter(out);
                pw.print("HTTP/1.0 " + status + " \r\n");

                if (mime != null) {
                    pw.print("Content-Type: " + mime + "\r\n");
                }

                if (header == null || header.getProperty("Date") == null) {
                    pw.print("Date: " + gmtFrmt.format(new Date()) + "\r\n");
                }

                if (header != null) {
                    Enumeration<Object> e = header.keys();
                    while (e.hasMoreElements()) {
                        String key = (String) e.nextElement();
                        String value = header.getProperty(key);
                        pw.print(key + ": " + value + "\r\n");
                    }
                }

                pw.print("\r\n");
                pw.flush();

                if (data != null) {
                    out.write(data.getBytes());
                }
                out.flush();
                out.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
                try {
                    socket.close();
                } catch (Throwable t) {
                }
            }
        }
    };
}
