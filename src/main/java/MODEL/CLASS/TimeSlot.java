package MODEL.CLASS;

public class TimeSlot {
    private Integer time_slot_id;
    private String time_range; // e.g., "8am-9am"

    // Constructors
    public TimeSlot() {
    	// Default constructor for empty TimeSlot object
    }
    
    public TimeSlot(Integer time_slot_id, String time_range) {
        this.time_slot_id = time_slot_id;
        this.time_range = time_range;
    }

    // Getters and Setters
    public Integer getTimeSlotId() {
        return time_slot_id;
    }

    public void setTimeSlotId(Integer time_slot_id) {
        this.time_slot_id = time_slot_id;
    }

    public String getTimeRange() {
        return time_range;
    }

    public void setTimeRange(String time_range) {
        this.time_range = time_range;
    }
}