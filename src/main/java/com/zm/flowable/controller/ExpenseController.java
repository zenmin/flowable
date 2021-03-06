package com.zm.flowable.controller;

import org.flowable.bpmn.model.*;
import org.flowable.bpmn.model.Process;
import org.flowable.engine.*;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.impl.persistence.entity.HistoricActivityInstanceEntityImpl;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.image.ProcessDiagramGenerator;
import org.flowable.task.api.Task;
import org.flowable.variable.api.history.HistoricVariableInstanceQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Describle This Class Is
 * @Author ZengMin
 * @Date 2019/2/15 10:42
 */
@RestController
public class ExpenseController {

    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private ProcessEngine processEngine;

    /**
     * 添加报销
     *
     * @param userId    用户Id
     * @param money     报销金额
     * @param descption 描述
     */
    @RequestMapping(value = "/add")
    @ResponseBody
    public String addExpense(String userId, Integer money, String descption) {
        //启动流程
        HashMap<String, Object> map = new HashMap<>();
        map.put("taskUser", userId);
        map.put("money", money);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("Expense", map);
        return "提交成功.流程Id为：" + processInstance.getId();
    }


    /**
     * 获取审批管理列表
     */
    @RequestMapping(value = "/list")
    @ResponseBody
    public String list(String userId) {
        List<Task> tasks = taskService.createTaskQuery().taskAssignee(userId).orderByTaskCreateTime().desc().list();
        StringBuffer t = new StringBuffer();
        for (Task task : tasks) {
            System.out.println(task.toString());
            t.append(task.toString());
        }
        return t.toString();
    }


    /**
     * 批准
     *
     * @param taskId 任务ID
     */
    @RequestMapping(value = "/apply")
    @ResponseBody
    public String apply(String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            return "task不存在";
        }
        //通过审核
        HashMap<String, Object> map = new HashMap<>();
        map.put("outcome", "通过");
        taskService.setVariableLocal(taskId,"outcome","通过");
        taskService.setVariableLocal(taskId,"reslover","zm");
        taskService.complete(taskId, map);
        return "processed ok!";
    }

    /**
     * 拒绝
     */
    @ResponseBody
    @RequestMapping(value = "/reject")
    public String reject(String taskId) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("outcome", "驳回");
        taskService.setVariableLocal(taskId,"outcome","驳回");
        taskService.setVariableLocal(taskId,"reslover","zm");
        taskService.complete(taskId, map);
        return "processed reject";
    }

    /**
     * 流程详情
     */
    @ResponseBody
    @RequestMapping(value = "/info")
    public Object info(String processId) {
        ProcessInstance pi = runtimeService.createProcessInstanceQuery().processInstanceId(processId).singleResult();
        Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        //使用流程实例ID，查询正在执行的执行对象表，返回流程实例对象
        String InstanceId = task.getProcessInstanceId();
        List<Execution> executions = runtimeService
                .createExecutionQuery()
                .processInstanceId(InstanceId)
                .list();
        HistoryService historyService = processEngine.getHistoryService();

        //历史详情  包含TASK步骤
        List<HistoricActivityInstance> activities =
                        historyService.createHistoricActivityInstanceQuery()
                        .processInstanceId(pi.getId())
                        .finished()
                        .orderByHistoricActivityInstanceEndTime().asc()
                        .listPage(0,100);

        // 历史概况
        List<HistoricActivityInstance> historicActivityInstances = activities.stream().filter(o -> o.getActivityName() != null).collect(Collectors.toList());
        List<Map<String,Object>> tasks = new ArrayList<>();

        // task参数
        HistoricVariableInstanceQuery historicVariableInstanceQuery = historyService.createHistoricVariableInstanceQuery();

        historicActivityInstances.stream().forEach(h -> {

            Map<String,Object> map = new LinkedHashMap<>();
            String taskId = h.getTaskId();
            map.put("步骤id",h.getTaskId());
            map.put("根流程id",h.getProcessInstanceId());
            map.put("是否是初始或结束步骤",((HistoricActivityInstanceEntityImpl) h).getRevision() == 1);
            map.put("activityId",h.getActivityId());
            map.put("activityName",h.getActivityName());
            map.put("activityType",h.getActivityType());
            map.put("持续时间",h.getDurationInMillis()+"ms");
            if(taskId != null){
                HistoricVariableInstanceQuery his = historicVariableInstanceQuery.taskId(h.getTaskId());
                Map<String,Object> map1 = new HashMap<>();
                map1.put("outcome",his.variableName("outcome").singleResult().getValue());
                map1.put("reslover",his.variableName("reslover").singleResult().getValue());
                map.put("步骤参数", map1);
            }
            tasks.add(map);
        });

        Map<String,Object> map =  new LinkedHashMap<>();
        map.put("流程实例id",pi.getId());
        map.put("流程标识key",pi.getProcessDefinitionKey());
        map.put("当前步骤（Task）id",task.getId());
        map.put("当前步骤",task.getName());
        map.put("历史步骤",tasks);
        // 获取流程配置信息

        BpmnModel bpmnModel = repositoryService.getBpmnModel(pi.getProcessDefinitionId());
        Process process = bpmnModel.getProcesses().get(0);
        Collection<FlowElement> flowElements = process.getFlowElements();
        List<Map<String,Object>> flows = new ArrayList<>();
        for (FlowElement e : flowElements){
            if(!StringUtils.isEmpty(e.getName())){      //过滤掉网关
                    Map<String,Object> m = new HashMap<>();
                    m.put("ActivityId",e.getId());
                    m.put("步骤名称",e.getName());
                    flows.add(m);
            }
        }
        map.put(process.getDocumentation(),flows);
        return map;
    }

    /**
     * 生成流程图
     *
     * @param processId 任务ID
     */
    @RequestMapping(value = "/processDiagram")
    public void genProcessDiagram(HttpServletResponse httpServletResponse, String processId) throws Exception {
        ProcessInstance pi = runtimeService.createProcessInstanceQuery().processInstanceId(processId).singleResult();

        //流程走完的不显示图
        if (pi == null) {
            return;
        }
        Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        //使用流程实例ID，查询正在执行的执行对象表，返回流程实例对象
        String InstanceId = task.getProcessInstanceId();
        List<Execution> executions = runtimeService
                .createExecutionQuery()
                .processInstanceId(InstanceId)
                .list();

        //得到正在执行的Activity的Id
        List<String> activityIds = new ArrayList<>();
        List<String> flows = new ArrayList<>();
        for (Execution exe : executions) {
            List<String> ids = runtimeService.getActiveActivityIds(exe.getId());
            activityIds.addAll(ids);
        }

        //获取流程图
        BpmnModel bpmnModel = repositoryService.getBpmnModel(pi.getProcessDefinitionId());
        ProcessEngineConfiguration engconf = processEngine.getProcessEngineConfiguration();
        ProcessDiagramGenerator diagramGenerator = engconf.getProcessDiagramGenerator();
        InputStream in = diagramGenerator.generateDiagram(bpmnModel, "png", activityIds, flows, engconf.getActivityFontName(), engconf.getLabelFontName(), engconf.getAnnotationFontName(), engconf.getClassLoader(), 1.0);
        OutputStream out = null;
        byte[] buf = new byte[1024];
        int legth = 0;
        try {
            out = httpServletResponse.getOutputStream();
            while ((legth = in.read(buf)) != -1) {
                out.write(buf, 0, legth);
            }
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        }
    }


}
