package cn.hhdxt.dingcheckinjava.properties;

import lombok.Data;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "info")
@Data
public class accountProperties {

    public List<Map<String,String>> userAccount;


}
