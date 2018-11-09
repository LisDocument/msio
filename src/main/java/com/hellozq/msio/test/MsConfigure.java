package com.hellozq.msio.test;

import com.hellozq.msio.bean.db.ForwardingRecode;
import com.hellozq.msio.bean.others.FileInterceptItem;
import com.hellozq.msio.config.AbstractMsConfigure;
import com.hellozq.msio.config.MsIoContainer;
import com.hellozq.msio.config.derivative.BaseInterceptConstruction;
import org.springframework.context.annotation.Configuration;

/**
 * Created with IntelliJ IDEA.
 *
 * @author ThisLi(Bin)
 * @date 2018/9/29
 * time: 10:47
 * To change this template use File | Settings | File Templates.
 */
@Configuration
public class MsConfigure extends AbstractMsConfigure {

    @Override
    public BaseInterceptConstruction addInterceptors(BaseInterceptConstruction intercept) {
        intercept.add("/xx/**",new FileInterceptItem());
        intercept.add("/xs/**",new FileInterceptItem());
        return super.addInterceptors(intercept);
    }

    @Override
    public MsIoContainer configContainer(MsIoContainer container) throws Exception {
        container.addMapping(ForwardingRecode.class);
        container.addMapping(User.class);
        return super.configContainer(container);
    }
}
