package skipper.store;

import lombok.Value;

@Value
public class PartitionConfig {
    int numberOfPartitions;
    int currentPartition;
}
