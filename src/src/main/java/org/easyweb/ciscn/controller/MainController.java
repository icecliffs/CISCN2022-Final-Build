package org.easyweb.ciscn.controller;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.UnknownAccountException;
import java.util.Base64;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.easyweb.ciscn.model.LoginForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import redis.clients.jedis.Jedis;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;


@RestController
public class MainController {
    Jedis jedis = new Jedis("127.0.0.1", 6379);
    private static final transient Logger log = LoggerFactory.getLogger(MainController.class);
    @PostMapping("/login")
    public String login(@ModelAttribute LoginForm loginForm){
        Map<String,String> errorMsg = new HashMap<>();
        Subject currentUser = SecurityUtils.getSubject();
        Session session = currentUser.getSession();
        if (!currentUser.isAuthenticated()) {
            UsernamePasswordToken token = new UsernamePasswordToken(loginForm.getUsername(), loginForm.getPassword());
            try {
                currentUser.login(token);
                checkSession(session);
                return "<meta http-equiv=\"refresh\" content=\"1.5; url=/\">Login Succeed!";
            } catch (UnknownAccountException uae) {
                log.info("User doesn't exist" + token.getPrincipal());
                errorMsg.put("errorMsg", "User doesn't exist!");
            } catch (IncorrectCredentialsException ice) {
                log.info("Password incorrent" + token.getPrincipal());
                errorMsg.put("errorMsg", "Wrong password!");
            } catch (AuthenticationException ae){
                log.info("Login Failed!" + token.getPrincipal());
                errorMsg.put("errorMsg", "Login Failed!");
            }
        }
        String htmlTemplate =
                "<!DOCTYPE html>" +
                "<html>" +
                "     <head>" +
                "          <title>Panel</title>" +
                "          <script src=\"./assert/jquery.js\"></script>" +
                "          <script type=\"text/javascript\">" +
                "               $(document).ready(function(){" +
                "                    $(\"#sb\").click(function(){" +
                "                         $.ajax({url:\"/api/show\",success:function(result){" +
                "                              $(\"#info\").html(result);" +
                "                         }});" +
                "                    });" +
                "                    $(\"#cl\").on('click', function(){" +
                "                         $.ajax({" +
                "                              url:\"/api/flushdb\"," +
                "                              success:function(result){" +
                "                                   alert(\"Done!\");" +
                "                              }," +
                "                              error: function(){" +
                "                                   alert(\"Error!\");" +
                "                              }" +
                "                         });" +
                "                    });" +
                "                    $(\"#pb\").click(function(){" +
                "                         $.ajax({url:\"/gifts\",success:function(result){" +
                "                              $(\"#flag\").html(result);" +
                "                         }});" +
                "                    });" +
                "               });" +
                "          </script>" +
                "          <style>" +
                "               body{" +
                "                    background: rgb(226, 226, 229);" +
                "               }" +
                "               .container{" +
                "                    margin: 0 auto;" +
                "                    padding: 10px;" +
                "                    margin-top: 100px;" +
                "                    background-color: rgb(255,255,255);" +
                "                    width: 900px;" +
                "                    border-radius: 20px;" +
                "                    box-shadow: 0px 0px 10px 1px;" +
                "                    height: auto;" +
                "               }" +
                "               input{" +
                "                    border-radius: 10px;" +
                "                    border: none;" +
                "                    outline: none;" +
                "                    box-shadow: 1px 1px 3px;" +
                "                    margin: 10px;" +
                "                    padding: 10px;" +
                "               }" +
                "               button{" +
                "                    border-radius: 10px;" +
                "                    border: none;" +
                "                    outline: none;" +
                "                    box-shadow: 1px 1px 3px;" +
                "                    margin: 10px;" +
                "                    padding: 10px;" +
                "               }" +
                "               a{" +
                "                    color: black;" +
                "                    text-decoration: none;" +
                "               }" +
                "          </style>" +
                "     </head>" +
                "     <body>" +
                "          <div class=\"container\">" +
                "               <p>Welcome <del>" + loginForm.getUsername() + "</del></p>" +
                "               <p>Session: " + session.getId() + "</p>" +
                "               <div class=\"box\">" +
            "                    <h1>Console</h1>" +
            "                         <p>Add</p>" +
                        "<form action=\"/api/add\" method=\"post\">" +
            "                         <input type=\"text\" name=\"key\" id=\"key\" placeholder=\"Key\">" +
            "                         <input type=\"text\" name=\"value\" id=\"value\" placeholder=\"Key-Value\">" +
            "                         <input type=\"submit\" value=\"Add\" id=\"Add\">" +
                        "</form>" +
                "                    <p>Show</p>" +
                "                    <button id=\"sb\">List</button>" +
                "                    <pre id=\"info\"></pre>" +
                "                    <p>Flush</p>" +
                "                    <button id=\"cl\">Flushdb</button>" +
                "                    <p>Check the gifts</p>" +
                "                    <p>If u want a gift, Plz generate a /gifts locally</p>" +
                "                    <pre id=\"flag\"></pre>" +
                "                    <button id=\"pb\">Check</button>" +
                "                    <br><a href=\"/logout\">Logout</a>" +
                "               </div>" +
                "          </div>" +
                "     </body>" +
                "</html>";
                checkAuthorization(currentUser);
        return htmlTemplate;
    }

    @GetMapping("/flag")
    public String fakeflag(){
        return "flag{Th1s_1s_Fake_8ut_Why_s0_Ser1ous?}";
    }

    @GetMapping("/logout")
    public String logout() {
        Subject currentUser = SecurityUtils.getSubject();
        currentUser.logout();
        return "<meta http-equiv=\"refresh\" content=\"1; url=/\">Bye!";
    }
    final Base64.Decoder decoder = Base64.getDecoder();

    @RequestMapping("/api/add")
    public void Console(String str, HttpServletRequest request, HttpServletResponse response) throws IOException, ClassNotFoundException, InterruptedException {
        PrintWriter writer = response.getWriter();
        StringBuffer html = new StringBuffer(); // String buffer require
        String key = request.getParameter("key");
        String value = request.getParameter("value");
        Subject currentUser = SecurityUtils.getSubject();
        if (!currentUser.isAuthenticated()) {
            writer.println("Unauthorized Access!");
            writer.flush();
        }else{
            if (value == "" && key == "") {
                html.append("ERROR!");
                writer.println(html);
                writer.flush();
            } else {
                FileWriter poc;
                poc = new FileWriter("/opt/poc.txt");
                poc.write(key + "\n");
                poc.write(value);
                poc.flush();
                poc.close();
                Process proc;
                try {
                    proc = Runtime.getRuntime().exec("bash /checkpoc.sh");
                    BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                    String line = null;
                    while ((line = in.readLine()) != null) {
                        System.out.println(line);
                    }
                    in.close();
                    proc.waitFor();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                writer.println("DONE!");
                writer.flush();
             }
        }
    }
    @RequestMapping("/api/flushdb")
    public void FlushDB(HttpServletResponse response) throws IOException{
        PrintWriter writer = response.getWriter();
        Subject currentUser = SecurityUtils.getSubject();
        if (!currentUser.isAuthenticated()) {
            writer.println("Unauthorized Access!");
            writer.flush();
        }else{
            jedis.flushDB();
            jedis.close();
        }
    }

    @RequestMapping("/api/show")
    public void ShowDB(HttpServletResponse response) throws IOException{
        Set<String> keys = jedis.keys("*");
        Iterator<String> it=keys.iterator() ;
        PrintWriter writer = response.getWriter();
        StringBuffer html = new StringBuffer(); // String buffer require
        Subject currentUser = SecurityUtils.getSubject();
        if (!currentUser.isAuthenticated()) {
            writer.println("Unauthorized Access!");
            writer.flush();
        }else{
            html.append("<pre>");
            while (it.hasNext()) {
                String key = it.next();
                html.append(key + "<br>");
            }
            html.append("</pre>");
            writer.println(html);
            writer.flush();
        }
    }

    @GetMapping("/gifts")
    public void CheckGifts(HttpServletResponse response) throws IOException{
        File file = new File("/gifts");
        PrintWriter writer = response.getWriter();
        if (file.exists()) {
            Process p = Runtime.getRuntime().exec("cat /flag");
            InputStream fis = p.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            String line = null;
            while ((line = br.readLine()) != null) {
                writer.println(line);
                writer.flush();
            }
        }else{
            writer.println("No gifts, No flag!");
            writer.flush();
        }

    }

    private void checkSession(Session session) {
        // Try to set value to redis-based session
        session.setAttribute("someKey", "aValue");
        String value = (String) session.getAttribute("someKey");
        if (!value.equals("aValue")) {
            log.info("Cannot retrieved the correct value! [" + value + "]");
        }
    }

    private void checkAuthorization(Subject currentUser) {
        // say who they are:
        // print their identifying principal (in this case, a username):
        log.info("User [" + currentUser.getPrincipal() + "] logged in successfully.");
        //test a role:
        if ( currentUser.hasRole( "schwartz" ) ) {
            log.info("May the Schwartz be with you!" );
        } else {
            log.info( "Hello, mere mortal." );
        }
    }
}
