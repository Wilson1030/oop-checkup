package com.oopc.rules;

import com.oopc.ir.Ir;
import com.oopc.report.CheckItem;
import com.oopc.report.Finding;
import com.oopc.report.Lang;

import java.util.List;

/** A check rule. One rule corresponds to one checklist item. */
public interface Rule {

    CheckItem item();

    List<Finding> apply(Ir.Project project, ScaleProfile scale, Lang lang);
}
