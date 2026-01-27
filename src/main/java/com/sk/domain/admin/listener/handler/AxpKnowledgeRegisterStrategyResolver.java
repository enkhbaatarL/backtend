package com.sk.domain.admin.listener.handler;

import com.sk.domain.admin.service.excel.DataCollectorStrategy;
import com.sk.domain.shared.enums.AxpKnowledgeRegisterComponentType;
import com.sk.domain.shared.enums.ComponentType;
import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AxpKnowledgeRegisterStrategyResolver {

    private final Map<String, AxpKnowledgeRegister> axpKnowledgeRegisters;
    private Map<AxpKnowledgeRegisterComponentType, AxpKnowledgeRegister> axpKnowledgeRegisterMap;

    @PostConstruct
    public void init(){
        axpKnowledgeRegisterMap = new HashMap<>();
        for(AxpKnowledgeRegister axpKnowledgeRegister : axpKnowledgeRegisters.values()){
            axpKnowledgeRegisterMap.put(axpKnowledgeRegister.getType(), axpKnowledgeRegister);
        }
    }
    public AxpKnowledgeRegister resolve(String componentType) {
        AxpKnowledgeRegisterComponentType axpKnowledgeRegisterComponentType = AxpKnowledgeRegisterComponentType.valueOf(componentType);
        AxpKnowledgeRegister axpKnowledgeRegister = axpKnowledgeRegisterMap.get(axpKnowledgeRegisterComponentType);
        if(axpKnowledgeRegister == null){
            axpKnowledgeRegisterMap.get(AxpKnowledgeRegisterComponentType.META);
        }
        return axpKnowledgeRegister;
    }
}
