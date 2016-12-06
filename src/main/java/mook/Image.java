package mook;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Image data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Image {
    private int id;
    private String name;
    private String caption;
}
