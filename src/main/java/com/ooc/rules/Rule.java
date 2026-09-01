package com.ooc.rules;

import com.ooc.ir.Ir;
import com.ooc.report.CheckItem;
import com.ooc.report.Finding;

import java.util.List;

/** 检测规则统一接口。一条规则对应检查表中的一项。 */
public interface Rule {

    CheckItem item();

    List<Finding> apply(Ir.Project project, ScaleProfile scale);
}
