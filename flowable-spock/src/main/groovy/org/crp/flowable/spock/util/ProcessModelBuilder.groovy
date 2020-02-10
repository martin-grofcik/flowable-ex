package org.crp.flowable.spock.util

import org.flowable.bpmn.model.*

/**
 * author martin.grofcik
 */
class ProcessModelBuilder {
    String name
    String id
    List<FlowElement> flowElements = []

    BpmnModel build() {
        return model(id, name, flowElements)
    }

    static ProcessModelBuilder model(String id = 'testProcess', String name = 'testProcessName') {
        return new ProcessModelBuilder(id: id, name: name)
    }

    static BpmnModel model(String id = 'testProcess', String name = 'testProcessName', List<FlowElement> flowElements) {
        BpmnModel model = new BpmnModel()
        Process process = new Process(id: id, name: name)
        model.addProcess(process)

        if (flowElements) {
            process.addFlowElement(flowElements[0])
            if (flowElements.size > 1) {
                (1..<flowElements.size).each {
                    def sourceRef = flowElements[it - 1].id
                    def targetRef = flowElements[it].id
                    process.addFlowElement(new SequenceFlow(sourceRef: sourceRef, targetRef: targetRef))
                    process.addFlowElement(flowElements[it])
                }
            }
        }

        return model
    }

    def rightShift(FlowElement flowElement) {
        flowElements << flowElement
        return this
    }

    static StartEvent startEvent() {
        return new StartEvent(id : 'startEvent')
    }

    static StartEvent startEvent(Map<String, Object> properties) {
        return new StartEvent(properties)
    }

    static EndEvent endEvent() {
        return new EndEvent(id : 'endEvent')
    }

    static EndEvent endEvent(Map<String, Object> properties) {
        return new EndEvent(properties)
    }

    static UserTask userTask(Map<String, Object> properties) {
        return new UserTask(properties)
    }

    static ScriptTask scriptTask(Map<String, Object> properties) {
        return new ScriptTask(properties)
    }

}
