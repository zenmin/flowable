package com.zm.flowable.taskhandler;

import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;

/**
 * @Describle This Class Is
 * @Author ZengMin
 * @Date 2019/2/15 10:40
 */
public class BossTaskHandler implements TaskListener {
    @Override
    public void notify(DelegateTask delegateTask) {
        delegateTask.setAssignee("老板审批");
    }
}
