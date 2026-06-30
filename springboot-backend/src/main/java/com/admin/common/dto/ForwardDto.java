package com.admin.common.dto;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;

@Data
public class ForwardDto {

    @NotBlank(message = "转发名称不能为空")
    private String name;
    
    @NotNull(message = "隧道ID不能为空")
    private Integer tunnelId;
    
    @NotBlank(message = "远程地址不能为空")
    private String remoteAddr;

    private String strategy;

    private Integer autoSwitchEnabled;

    @Min(value = 1, message = "失败阈值不能小于1")
    @Max(value = 20, message = "失败阈值不能大于20")
    private Integer autoSwitchFailThreshold;

    @Min(value = 1, message = "恢复阈值不能小于1")
    @Max(value = 20, message = "恢复阈值不能大于20")
    private Integer autoSwitchRecoverThreshold;

    @Min(value = 5, message = "健康检查间隔不能小于5秒")
    @Max(value = 300, message = "健康检查间隔不能大于300秒")
    private Integer healthCheckIntervalSec;

    @Min(value = 500, message = "健康检查超时不能小于500毫秒")
    @Max(value = 10000, message = "健康检查超时不能大于10000毫秒")
    private Integer healthCheckTimeoutMs;

    private String preferredTarget;
    
    /**
     * 入口端口（可选，为空时自动分配）
     */
    @Min(value = 1, message = "端口号不能小于1")
    @Max(value = 65535, message = "端口号不能大于65535")
    private Integer inPort;

    private String interfaceName;

} 