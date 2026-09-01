package com.ooc.rules;

import com.ooc.ir.Ir;
import com.ooc.report.Finding;

import java.util.List;

/** 检测规则统一接口。新增规则只需实现本接口并在 Main 中注册。 */
public interface Rule {

    String id();

    String name();

    List<Finding> apply(Ir.Project project, ScaleProfile scale);
}
