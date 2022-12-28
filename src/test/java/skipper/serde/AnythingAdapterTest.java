package skipper.serde;

import static org.junit.Assert.assertEquals;

import com.google.gson.GsonBuilder;
import java.util.ArrayList;
import java.util.List;
import lombok.val;
import org.junit.Test;
import skipper.api.SkipperWorkflow;
import skipper.common.Anything;
import skipper.models.*;

public class AnythingAdapterTest {
  @Test
  public void testAdapter() {
    val jsonBuilder = new GsonBuilder().registerTypeAdapter(Anything.class, new AnythingAdapter());
    List<WorkflowType> list = new ArrayList<>();
    list.add(new WorkflowType(SkipperWorkflow.class));
    list.add(null);
    Anything test = Anything.of(list, WorkflowType.class);
    String ser = jsonBuilder.create().toJson(test);
    Anything deser = jsonBuilder.create().fromJson(ser, Anything.class);
    assertEquals(test, deser);
  }
}
