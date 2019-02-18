package com.zm.flowable.taskhandler;

import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;

/**
 * @Describle This Class Is
 * @Author ZengMin
 * @Date 2019/2/15 10:38
 */
public class ManagerTaskHandler implements TaskListener {

    @Override
    public void notify(DelegateTask delegateTask) {
        delegateTask.setAssignee("经理审批");
    }
}
