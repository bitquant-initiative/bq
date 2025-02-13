package bq.util;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.assertj.core.api.Assertions;
import org.assertj.core.util.Lists;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class DependencyTest {

  
  
  private void checkNotArtifact(File pom, JsonNode dep, String artifactId) {
    Assertions.assertThat(dep.path("artifactId").asText()).withFailMessage("%s should not contain dependency: %s", pom,artifactId).isNotEqualTo(artifactId);
  }
  private void checkPom(File pom, Document d) {
    
    Element root = d.getRootElement();
    Namespace ns = root.getNamespace();
    String dirName = pom.getParentFile().getName();
    
    Assertions.assertThat(root.getChildText("artifactId",ns)).isEqualTo(dirName);
    Assertions.assertThat(root.getChildText("name",ns)).isEqualTo(dirName);
    
    String module = root.getChildText("artifactId",ns);
    List<JsonNode> jd = Lists.newArrayList();
    
    Element dependencies = d.getRootElement().getChild("dependencies",ns);
    if (dependencies==null) {
      return;
    }
    dependencies.getChildren().forEach(it->{
      
      ObjectNode dep = Json.createObjectNode();
      String artifactId= it.getChildText("artifactId",ns);
      String scope= it.getChildText("scope",ns);
      String groupId = it.getChildText("groupId",ns);
      String version = it.getChildText("version",ns);
      dep.put("groupId", groupId);
      dep.put("artifactId", artifactId);
      dep.put("version",version);
      dep.put("scope",scope);
      jd.add(dep);     
    });
    
    jd.forEach(dep->{
 
      if (!Set.of("bq-util","bq-test").contains(module)) {
        checkNotArtifact(pom, dep,"flogger");
        checkNotArtifact(pom, dep,"flogger-system-backend");
        checkNotArtifact(pom, dep,"slf4j-api");
        checkNotArtifact(pom, dep,"jackson-core");
        checkNotArtifact(pom, dep,"jackson-databind");
        checkNotArtifact(pom, dep,"jackson-dataformat-yaml");
      }
      
      if (dep.path("groupId").asText().equals("io.github.bitquant-initiative")) {
        Assertions.assertThat(dep.path("version")
            
            
            .asText()).withFailMessage("%s: %s should have version: ${revision}",pom,dep.path("artifactId").asText()).isEqualTo("${revision}");
      }
  
    });
    
    
  }
  @Test
  public void testIt() throws IOException, JDOMException, SAXException, ParserConfigurationException {
    
    
    for (File dir: new File("..").listFiles()) {
      
      File pom = new File(dir,"pom.xml");
      
      if (pom.exists()) {
        SAXBuilder b = new SAXBuilder();
        Document d = b.build(pom);
        

        Namespace ns = d.getRootElement().getNamespace();
        checkPom(pom,d);
  
        
      }
    }
    
    
  

  }
}
