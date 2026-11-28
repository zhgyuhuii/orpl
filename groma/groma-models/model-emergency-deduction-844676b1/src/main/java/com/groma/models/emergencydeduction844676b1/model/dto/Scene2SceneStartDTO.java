package com.groma.models.emergencydeduction844676b1.model.dto;

import com.groma.base.common.annotation.GromaMeta;
import com.groma.base.engine.GromaValues;
import java.lang.Boolean;
import java.lang.Long;
import java.lang.Object;
import java.lang.String;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 场景2 场景启动参数 DTO（设计说明书 3.1 跨层三件套收口）：强类型 seed / startPaused，
 * 动态想定覆盖键值归入 initialOverrides（键语义与 REST 启动参数完全一致）。
 */
@GromaMeta(
        id = "fFnAAr3WKsN0Oaghvzj",
        zhName = "场景2启动参数",
        kind = "SCENE_START_DTO"
)
public class Scene2SceneStartDTO {
    private Long seed;

    private Boolean startPaused;

    private Map<String, Object> initialOverrides = new LinkedHashMap<>(16);

    /**
     * REST Map 启动参数 → 强类型 DTO（线格式兼容既有契约；seed/startPaused 提升为字段）
     */
    public static Scene2SceneStartDTO fromMap(Map<String, Object> startParams) {
        Scene2SceneStartDTO dto = new Scene2SceneStartDTO();
        dto.seed = startParams == null || startParams.get("seed") == null ? null
                : (long) GromaValues.toLong(startParams.get("seed"));
        dto.startPaused = startParams != null && Boolean.TRUE.equals(startParams.get("startPaused"));
        if (startParams != null) {
            dto.initialOverrides.putAll(startParams);
        }
        return dto;
    }

    public Long getSeed() {
        return this.seed;
    }

    public void setSeed(Long seed) {
        this.seed = seed;
    }

    public Boolean getStartPaused() {
        return this.startPaused;
    }

    public void setStartPaused(Boolean startPaused) {
        this.startPaused = startPaused;
    }

    public Map<String, Object> getInitialOverrides() {
        return this.initialOverrides;
    }

    public void setInitialOverrides(Map<String, Object> initialOverrides) {
        this.initialOverrides = initialOverrides;
    }
}
