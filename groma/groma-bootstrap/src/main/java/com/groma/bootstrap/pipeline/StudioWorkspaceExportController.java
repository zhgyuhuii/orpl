package com.groma.bootstrap.pipeline;

import com.groma.base.common.Result;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 源码工程导出接口：GET /api/v1/studio/workspace/export-zip?models=model-a,model-b
 * 流式返回可独立 {@code mvn clean package} 的完整源码工程 ZIP（静态合包所选模型）。
 */
@RestController
@RequestMapping("/api/v1/studio/workspace")
public class StudioWorkspaceExportController {

    private final StudioWorkspaceExportService exportService;

    public StudioWorkspaceExportController(StudioWorkspaceExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping("/export-zip")
    public void exportZip(@RequestParam("models") String models, HttpServletResponse response) throws IOException {
        if (models == null || models.isBlank()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(String.valueOf(
                    Result.fail("EXPORT_BAD_REQUEST", "models 参数必填（逗号分隔的模型模块名）", null)));
            return;
        }
        List<String> modelList = Arrays.stream(models.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
        StudioWorkspaceExportService.ExportPlan plan;
        try {
            plan = exportService.plan(modelList);
        } catch (IllegalArgumentException badRequest) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(String.valueOf(
                    Result.fail("EXPORT_MODEL_NOT_FOUND", badRequest.getMessage(), null)));
            return;
        }
        String fileName = "groma-workspace-" + String.join("-", plan.modelModules()) + ".zip";
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        exportService.exportZip(plan, response.getOutputStream());
    }
}
