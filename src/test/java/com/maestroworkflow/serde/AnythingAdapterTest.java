package com.maestroworkflow.serde;

import static org.junit.Assert.assertEquals;

import com.google.gson.GsonBuilder;
import com.maestroworkflow.api.MaestroWorkflow;
import com.maestroworkflow.models.Anything;
import com.maestroworkflow.models.WorkflowType;
import java.util.ArrayList;
import java.util.List;
import lombok.val;
import org.junit.Test;

public class AnythingAdapterTest {
  @Test
  public void testAdapter() {
    val jsonBuilder = new GsonBuilder().registerTypeAdapter(Anything.class, new AnythingAdapter());
    List<WorkflowType> list = new ArrayList<>();
    list.add(new WorkflowType(MaestroWorkflow.class));
    Anything test = Anything.of(list, WorkflowType.class);
    String ser = jsonBuilder.create().toJson(test);
    Anything deser = jsonBuilder.create().fromJson(ser, Anything.class);
    assertEquals(test, deser);
  }
}
