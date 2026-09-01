package com.ooc.rules;

import com.ooc.ir.Ir;
import com.ooc.report.CheckItem;
import com.ooc.report.Finding;
import com.ooc.report.Lang;

import java.util.List;

/** A check rule. One rule corresponds to one checklist item. */
public interface Rule {

    CheckItem item();

    List<Finding> apply(Ir.Project project, ScaleProfile scale, Lang lang);
}
