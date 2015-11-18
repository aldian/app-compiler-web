package com.aldianfazrihady.controller;

import com.aldianfazrihady.model.CompilationResult;
import com.aldianfazrihady.model.User;
import com.aldianfazrihady.service.CompilationResultService;
import com.aldianfazrihady.service.UserService;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by AldianFazrihady on 11/13/15.
 */
@RestController
public class WebServices {
    @Autowired
    private UserService userService;

    @Autowired
    private CompilationResultService compilationResultService;

    @RequestMapping(value = "/ws/login", method = {RequestMethod.POST})
    public String login(@RequestParam("username") String username, @RequestParam("password") String password) {
        User user = userService.login(username, password);
        user = userService.generateWebServiceToken(user);
        return user.getWsToken();
    }

    @RequestMapping(value = "/ws/logout", method = {RequestMethod.POST})
    public void logout(@RequestParam("accessToken") String accessToken) {
        User user = userService.findByWsToken(accessToken);
        user.setWsToken(null);
        userService.update(user);
    }

    @Transactional
    @RequestMapping(value = "/ws/compile", method = {RequestMethod.POST})
    public CompilationResult compile(@RequestParam("accessToken") String accessToken, @RequestParam("zipFile") MultipartFile zipFile) throws Exception {
        User user = userService.findByWsToken(accessToken);
        if (user == null) {
            throw new Exception("Invalid accessToken");
        }
        String dirName = "compilation_space/" + user.getId();
        File dir = new File(dirName);
        if (dir.exists()) {
            FileUtils.deleteDirectory(dir);
        }
        dir.mkdirs();
        byte[] zipBytes = zipFile.getBytes();
        File fZip = new File(dirName + "/" + zipFile.getOriginalFilename());
        FileOutputStream fOut = new FileOutputStream(fZip);
        fOut.write(zipBytes);
        fOut.close();

        ZipFile zFile = new ZipFile(fZip);
        List<ZipArchiveEntry> entries = Collections.list(zFile.getEntries());
        byte[] buf = new byte[1024];
        List<String> javaFileNames = new ArrayList<String>();
        for (ZipArchiveEntry entry: entries) {
            InputStream inStream = zFile.getInputStream(entry);
            String entryName = entry.getName();
            String[] nameComps = entryName.split("[/\\\\]+");
            String parentDir = "";
            if (nameComps.length > 1) {
                for (int i = 0; i < nameComps.length - 1; ++i) {
                    parentDir += "/" + nameComps[i];
                    dir = new File(parentDir);
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }
                }
            }
            String javaFileName = parentDir + "/" + nameComps[nameComps.length - 1];
            File fJavaOut = new File(dirName + javaFileName);
            FileOutputStream fJavaOutStream = new FileOutputStream(fJavaOut);
            for (int off = 0, nRead = 0; (nRead = inStream.read(buf, 0, buf.length)) > 0; off += nRead) {
                fJavaOutStream.write(buf, 0, nRead);
            }
            fJavaOutStream.close();
            javaFileNames.add(javaFileName);
        }
        StringBuilder compilationResult = new StringBuilder();
        for (String javaFileName: javaFileNames) {
            String command = "javac " + javaFileName.substring(1) + "\n";
            compilationResult.append(command);
            String realCommand = "javac " + dirName + javaFileName;
            String output = executeCommand(realCommand);
            compilationResult.append(output);
        }
        //com.sun.tools.javac.Main.compile(javaFileNames.toArray(new String[1]));
        CompilationResult res = new CompilationResult(compilationResult.toString());
        res = compilationResultService.update(res);
        //user.getCompilationResults().add(res);
        //user = userService.update(user);
        return res;
    }


    private int executeCommand(String command, int i) {
        return 0;
    }

    private String executeCommand(String command) {
        StringBuilder output = new StringBuilder();

        Process p;
        try {
            p = Runtime.getRuntime().exec(command);
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = "";
            while ((line = reader.readLine()) != null) {
                output.append(line + "\n");
            }
            reader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            while ((line = reader.readLine()) != null) {
                output.append(line + "\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return output.toString();
    }
}
