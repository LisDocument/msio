package com.hellozq.msio.test;

import com.google.common.collect.Lists;
import com.hellozq.msio.anno.MsReturnTranslator;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/us")
public class TestController {

    @RequestMapping("/t")
    public Map test1(String title,String title2){
        return new HashMap(){
            {
                put("name","李");
                put("age","18");
            }
        };
    }


    @MsReturnTranslator("getPage()#getList($int$5,$int$150,wowowo,$double$1.41)")
    @RequestMapping("/t2")
    public PageContent test2(){
        List list =  Lists.newArrayList();
        for (int i = 0; i < 360000; i++) {
            HashMap<Object, Object> objectObjectHashMap = new HashMap<>();
            objectObjectHashMap.put("name","李");
            objectObjectHashMap.put("age",i + "");
            list.add(objectObjectHashMap);
        }
        return new PageContent(new Page(list));
    }
}
