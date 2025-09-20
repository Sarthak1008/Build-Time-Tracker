# Build-Time-Tracker

A Maven plugin that tracks and reports build times for different phases of your Maven build lifecycle.

## Features

- **Total Build Time**: Displays the overall build duration at the end
- **Phase Summary Table**: Prints a neat summary table of all phase durations
- **Threshold Warnings**: Warns if a phase takes longer than a configurable threshold
- **JSON/CSV Export**: Optionally exports timing data to JSON or CSV files

## Installation

### Using JitPack

Add the following to your `pom.xml`:

```xml
<plugin>
  <groupId>com.github.sarthakaggarwal</groupId>
  <artifactId>build-time-tracker</artifactId>
  <version>1.0.0</version>
  <executions>
    <execution>
      <goals>
        <goal>track</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

## Configuration Options

You can configure the plugin with the following options:

```xml
<plugin>
  <groupId>com.github.sarthakaggarwal</groupId>
  <artifactId>build-time-tracker</artifactId>
  <version>1.0.0</version>
  <executions>
    <execution>
      <goals>
        <goal>track</goal>
      </goals>
    </execution>
  </executions>
  <configuration>
    <!-- Warn if a phase takes longer than 5000ms (5 seconds) -->
    <warnThreshold>5000</warnThreshold>
    
    <!-- Export timing data to JSON -->
    <exportJson>true</exportJson>
    
    <!-- Export timing data to CSV -->
    <exportCsv>true</exportCsv>
    
    <!-- Custom output directory (defaults to ${project.build.directory}) -->
    <outputDirectory>${project.build.directory}/build-reports</outputDirectory>
  </configuration>
</plugin>
```

## Example Output

When your build completes, you'll see a summary table like this:

```
========== Build Time Summary ==========
Phase      | Duration (ms)
----------------------------
validate   | 150
compile    | 3201
test       | 1840
package    | 642
----------------------------
TOTAL      | 5833 ms
=======================================
```

If a phase exceeds the warning threshold, you'll see:

```
⚠️  Phase "test" took 12.4s (threshold = 10.0s)
```

## JSON Export

If enabled, the plugin will generate a JSON file at `target/build-time-report.json`:

```json
{
  "validate": 150,
  "compile": 3201,
  "test": 1840,
  "package": 642,
  "total": 5833
}
```

## CSV Export

If enabled, the plugin will generate a CSV file at `target/build-time-report.csv`:

```
Phase,Duration(ms)
validate,150
compile,3201
test,1840
package,642
total,5833
```

## Building from Source

```bash
git clone https://github.com/sarthakaggarwal/build-time-tracker.git
cd build-time-tracker
mvn clean install
```

## License

This project is licensed under the MIT License - see the LICENSE file for details.