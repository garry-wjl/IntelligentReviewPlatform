package com.audit.platform.domain.scene.valueobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SceneParamVO {
    /** 参数键，内置为 Input / Attachment */
    private String key;
    /** 展示名称 */
    private String label;
    /** INPUT / ATTACHMENT / STRING */
    private String type;
    /** 是否内置默认参数 */
    private Boolean builtin;
}
