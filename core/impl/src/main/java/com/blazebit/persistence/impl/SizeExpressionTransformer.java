package com.blazebit.persistence.impl;

import com.blazebit.persistence.impl.expression.Expression;


public class SizeExpressionTransformer implements ExpressionTransformer {
    private final SizeTransformationVisitor sizeTransformationVisitor;
    private final SelectManager<?> selectManager;
    
    public SizeExpressionTransformer(SizeTransformationVisitor sizeTransformationVisitor, SelectManager<?> selectManager) {
        this.sizeTransformationVisitor = sizeTransformationVisitor;
        this.selectManager = selectManager;
    }
    
    @Override
    public Expression transform(Expression original, ClauseType fromClause, boolean joinRequired) {
        // select clause is transformed separately
        if (fromClause != ClauseType.SELECT) {
            sizeTransformationVisitor.setClause(fromClause);
            sizeTransformationVisitor.setOrderBySelectClause(false);
            boolean[] groupBySelectStatus = selectManager.containsGroupBySelect(true);
            sizeTransformationVisitor.setHasGroupBySelects(groupBySelectStatus[0]);
            sizeTransformationVisitor.setHasComplexGroupBySelects(groupBySelectStatus[1]);
            return original.accept(sizeTransformationVisitor);
        } else {
            return original;
        }
    }

}