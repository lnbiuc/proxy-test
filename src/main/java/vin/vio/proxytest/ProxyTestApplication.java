package vin.vio.proxytest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;

@SpringBootApplication
public class ProxyTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProxyTestApplication.class, args);
    }

}


@RestController
@Slf4j
class ProxyController {


    @RequestMapping("/api/hello")
    public ResponseEntity<String> hello(HttpServletRequest request) {
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            log.info("{}: {}", headerName, headerValue);
        }
        return ResponseEntity.ok("Hello");
    }

    @RequestMapping("/api/hello/{name}")
    public ResponseEntity<String> helloPath(@PathVariable String name) {

        log.info("{}: {}", name, "Hello");
        return ResponseEntity.ok("Hello");
    }

    @RequestMapping("/api/hello/query")
    public ResponseEntity<String> helloQuery(@RequestParam String name) {

        log.info("{}: {}", name, "Hello");
        return ResponseEntity.ok("Hello");
    }

    @RequestMapping("/api/auth")
    public ResponseEntity<String> helloAuth(HttpServletRequest request) {
        String userid = request.getHeader("userid");
        if (userid != null) {
            int id = Integer.parseInt(userid);
            if (id % 2 == 0) {
                return ResponseEntity.ok("1");
            }
        }
        // 403
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden");
    }

    private static final String UPLOAD_DIR = "image/";

    @PostMapping("/api/file/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        // 获取系统临时目录
        String tempDir = System.getProperty("java.io.tmpdir");
        File directory = new File(tempDir, UPLOAD_DIR);

        // 创建文件夹如果不存在
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // 保存文件
        try {
            String fileName = file.getOriginalFilename();
            File destFile = new File(directory, fileName);
            file.transferTo(destFile);
            return ResponseEntity.ok(fileName);  // 返回文件名
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("File upload failed: " + e.getMessage());
        }
    }

    @GetMapping("/api/file/download/{filename}")
    public ResponseEntity<String> downloadFile(@PathVariable("filename") String filename, HttpServletResponse response) {
        // 获取系统临时目录
        String tempDir = System.getProperty("java.io.tmpdir");
        File file = new File(tempDir, UPLOAD_DIR + filename);

        // 检查文件是否存在
        if (!file.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found");
        }

        // 设置响应头
        response.setContentType("application/octet-stream");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"");

        // 写入文件内容到响应流
        try (FileInputStream inStream = new FileInputStream(file);
             OutputStream outStream = response.getOutputStream()) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inStream.read(buffer)) != -1) {
                outStream.write(buffer, 0, bytesRead);
            }
            outStream.flush(); // 确保所有内容被写入
            return ResponseEntity.ok().build(); // 由于文件已经写入，返回204
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("File download failed: " + e.getMessage());
        }
    }
}