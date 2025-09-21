# Build-Time-Tracker

An advanced Maven plugin that tracks and analyzes build times with comprehensive analytics, bottleneck detection, and performance monitoring.

## Features

### Core Features
- **Total Build Time**: Displays the overall build duration at the end
- **Phase-wise Duration Tracking**: Detailed timing for each Maven lifecycle phase
- **Colored Console Output**: Visual indicators for fast, normal, and slow phases
- **JSON/CSV Export**: Export timing data in multiple formats
- **HTML Dashboard**: Beautiful interactive dashboard with charts and analytics

### Advanced Analytics
- **🎯 Bottleneck Analysis**: Identifies performance bottlenecks and optimization opportunities
- **📈 Regression Detection**: Detects performance regressions compared to build history
- **⭐ Build Efficiency Scoring**: Comprehensive scoring system with letter grades
- **🖥️ System Resource Monitoring**: Real-time CPU and memory usage tracking
- **📊 Interactive Charts**: Visual representation of phase breakdowns and trends
- **💡 Smart Recommendations**: AI-powered suggestions for build optimization

## Installation

### Maven Central

Add the following to your `pom.xml`:

```xml
<plugin>
	<groupId>io.github.sarthak1008</groupId>
	<artifactId>build-time-tracker</artifactId>
	<version>1.0.5</version>
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
	<groupId>io.github.sarthak1008</groupId>
	<artifactId>build-time-tracker</artifactId>
	<version>1.0.5</version>
	<executions>
		<execution>
			<goals>
				<goal>track</goal>
			</goals>
		</execution>
	</executions>
	<configuration>
		<!-- Performance Thresholds -->
		<warnThreshold>5000</warnThreshold>          <!-- Warn if phase > 5s -->
		<fastThreshold>1000</fastThreshold>          <!-- Fast phase < 1s -->
		
		<!-- Output Options -->
		<generateHtml>true</generateHtml>            <!-- Generate HTML dashboard -->
		<coloredOutput>true</coloredOutput>          <!-- Enable colored console output -->
		<outputDirectory>${project.build.directory}</outputDirectory>
		
		<!-- Advanced Analytics -->
		<enableSystemMonitoring>true</enableSystemMonitoring>     <!-- Monitor CPU/Memory -->
		<enableBottleneckAnalysis>true</enableBottleneckAnalysis> <!-- Bottleneck detection -->
		<monitoringInterval>1000</monitoringInterval>             <!-- Monitoring interval (ms) -->
		<regressionThreshold>1.5</regressionThreshold>           <!-- 1.5x slower = regression -->
		<historySize>20</historySize>                             <!-- Keep last 20 builds -->
		
		<!-- History Management -->
		<historyFile>${project.build.directory}/build-history.json</historyFile>
	</configuration>
</plugin>
```

## Example Output

### Console Output

When your build completes, you'll see enhanced analytics output:

```
🚀 Advanced Build Time Tracker initialized with analytics engine...
   📊 Bottleneck Analysis: ENABLED
   📈 System Monitoring: ENABLED
   🔍 Regression Detection: ENABLED (threshold: 1.5x)

⚙️  Starting phase: validate
✅ ⚡ Phase "validate" completed (0.2s)
⚙️  Starting phase: compile
✅ ⚠️ Phase "compile" completed (3.2s)
⚙️  Starting phase: test
✅ 🐌 Phase "test" completed (8.4s)
⚙️  Starting phase: package
✅ ⚡ Phase "package" completed (0.6s)

🔬 ADVANCED ANALYTICS REPORT
==================================================

🎯 BOTTLENECK ANALYSIS
──────────────────────────────
Primary Bottleneck: test (8.4s, 67.2% of build)
Top 3 Time Consumers:
  🔥 test: 8.4s (67.2%)
  ⚠️ compile: 3.2s (25.6%)
  📊 package: 0.6s (4.8%)

💡 Optimization Recommendations:
  • Focus optimization on 'test' phase (major bottleneck)
  • Optimize test execution with parallel runners or test selection

📈 REGRESSION ANALYSIS
──────────────────────────────
✅ Build performance is stable
Current: 12.5s | Average: 11.8s
Trend: Variable | Builds analyzed: 15

⭐ BUILD EFFICIENCY SCORE
──────────────────────────────
Overall Score: 78.5/100 (B)
Score Breakdown:
  ⏱️  Time Efficiency: 22.1/30 pts
  💾 Memory Efficiency: 20.0/25 pts
  🖥️  CPU Efficiency: 18.4/25 pts
  📊 Consistency: 18.0/20 pts

🖥️ SYSTEM RESOURCE ANALYSIS
──────────────────────────────
Memory Usage: 512.3 MB avg, 1024.7 MB peak
CPU Usage: 65.2% avg, 89.1% peak
GC Activity: 12 collections, 0.8s total time
==================================================

🎯 Build dashboard generated: /target/build-dashboard.html
```

## HTML Dashboard

The plugin generates a comprehensive HTML dashboard at `target/build-dashboard.html` featuring:

- **📊 Interactive Charts**: Doughnut charts showing phase breakdown
- **📈 Performance Metrics**: Key metrics cards with visual indicators
- **🎯 Analytics Section**: Bottleneck analysis, regression detection, and efficiency scoring
- **📋 Detailed Phase Table**: Sortable table with duration, percentage, and status
- **💡 Smart Recommendations**: Actionable optimization suggestions
- **🖥️ System Resource Analysis**: Memory and CPU usage insights

### Dashboard Features
- Responsive design that works on all devices
- Real-time data visualization with Chart.js
- Color-coded performance indicators
- Historical trend analysis
- Export capabilities for further analysis

## Build History & Analytics

The plugin maintains a build history file (`build-history.json`) that enables:

- **Regression Detection**: Automatically detects when builds become significantly slower
- **Performance Trends**: Tracks performance over time to identify patterns
- **Efficiency Scoring**: Comprehensive scoring based on time, memory, CPU, and consistency
- **Historical Comparisons**: Compare current build against recent averages

### Build History Format

```json
{
  "builds": [
    {
      "timestamp": "2024-01-15T10:30:45",
      "totalTime": 12450,
      "memoryUsage": 1073741824,
      "cpuUsage": 0.652
    }
  ]
}
```

## Configuration Parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| `warnThreshold` | 5000 | Threshold in ms for warning about slow phases |
| `fastThreshold` | 1000 | Threshold in ms for considering a phase fast |
| `generateHtml` | true | Generate HTML dashboard |
| `coloredOutput` | true | Enable colored console output |
| `outputDirectory` | `${project.build.directory}` | Directory for generated reports |
| `enableSystemMonitoring` | true | Monitor CPU and memory usage |
| `enableBottleneckAnalysis` | true | Enable bottleneck detection |
| `monitoringInterval` | 1000 | System monitoring interval in ms |
| `regressionThreshold` | 1.5 | Factor for detecting performance regression |
| `historySize` | 20 | Number of builds to keep in history |
| `historyFile` | `${project.build.directory}/build-history.json` | Build history file location |

## Performance Tips

### Optimizing Based on Analytics

1. **Bottleneck Analysis**: Focus on the phases that consume the most time
2. **System Monitoring**: Ensure adequate memory allocation if peak usage is high
3. **Regression Detection**: Investigate sudden performance drops
4. **Efficiency Score**: Aim for scores above 85 for optimal performance

### Common Optimizations

- **Compilation**: Use incremental compilation, compiler daemon
- **Testing**: Implement parallel test execution, test selection strategies
- **Dependencies**: Optimize dependency resolution, use local repositories
- **Memory**: Increase heap size if memory usage is consistently high
- **CPU**: Enable parallel builds with `-T` flag

## Building from Source

```bash
git clone https://github.com/sarthak1008/build-time-tracker.git
cd build-time-tracker
mvn clean install
```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request. For major changes, please open an issue first to discuss what you would like to change.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Changelog

### Version 1.0.5
- Added advanced analytics engine with bottleneck analysis
- Implemented system resource monitoring (CPU/Memory)
- Added regression detection with build history tracking
- Introduced build efficiency scoring system
- Created interactive HTML dashboard with charts
- Enhanced console output with colored indicators
- Added smart optimization recommendations

### Version 1.0.0
- Initial release with basic build time tracking
- Phase-wise duration measurement
- JSON/CSV export functionality
- Warning thresholds for slow phases