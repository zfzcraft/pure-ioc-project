# pure-ioc-project
Pure IOC is a new-generation, minimalist, high-performance IOC container redefined from the ground up, cloud-native oriented and deeply optimized for Kubernetes environments.

# Pure IOC Design Philosophy
Pure IOC is a **new-generation, minimalist, high-performance IOC container redefined from the ground up, cloud-native oriented and deeply adapted for Kubernetes environments**.

It completely abandons the bloated, redundant, black-box, complex, and highly intrusive designs of traditional frameworks, adhering to the core principles of **minimalist kernel, transparent controllability, low memory footprint, high concurrency, and no black magic**. It retains only the most essential Bean management and dependency injection capabilities, built as a stable, clean, and deeply debuggable lightweight container foundation for microservices, containerization, elastic scheduling, and high-concurrency, low-latency scenarios.

1. **Single Container Architecture, Minimalist Without Nesting**
There is **only one global ApplicationContext**, with no parent-child containers, no abstract hierarchies, and no multi-layer container nesting. The architecture is extremely clean, stable, transparent, easy to maintain, and easy to troubleshoot.

2. **Global Singleton Scope, Cloud-Native Stateless Standard**
Following the stateless design principle of microservices, **only singleton scope is supported**. All redundant scope designs are removed, eliminating unnecessary object creation, resulting in extremely low memory usage and stable, efficient runtime.

3. **Minimalist BeanDefinition, No Redundant Complex Processing**
Lightweight Bean metadata model, **no complex dependency checking, no dependency sorting, no redundant attribute logic**. Only core class information and instantiation rules are preserved, enabling faster startup, lower memory usage, and clearer logic.

4. **Class as the Unique Identifier, Type-Safe and Unambiguous**
Uses **Class as the unique identifier for Beans**, abandoning ambiguous byName and byType matching. For interfaces with a single implementation, use the interface Class; for multiple implementations, use the implementation Class. Fully type-safe, conflict-free, and unambiguous in configuration.

5. **Pure Constructor Injection, No Lifecycle Callbacks**
**Only constructor instantiation and dependency injection are supported**. Field injection and Setter injection are completely discarded. There are no initialization methods, no callbacks, and no lifecycle binding, fully complying with native Java specifications—clean and minimalist.

6. **Default Lazy Loading + Eager Loading for Core Components**
Global default **lazy loading** enables container startup in seconds, perfectly adapting to Kubernetes fast scheduling and elastic scaling.
For core infrastructure components such as **Servlet containers and Netty**, annotation-marked **eager loading** is supported to ensure core services are ready upon startup.

7. **Asynchronous Preheating After Startup, Balancing Startup Speed and Runtime Performance**
After the container completes fast startup, it asynchronously preheats all Beans. This ensures ultra-fast startup in cloud-native environments while achieving zero-latency, high-performance runtime.

8. **Multi-Threaded Parallel Class Loading, Greatly Improving Startup Efficiency**
For scenarios with large-scale class loading, multi-threaded parallel class loading is adopted, significantly reducing startup time for large applications with extreme performance optimization.

9. **Zero Package Scanning at Runtime, Full Support for Framework and Plugins**
Inefficient runtime package scanning is completely abandoned. Beans are defined via a **unified configuration file**, and a Bean index is automatically generated during the packaging phase. At runtime, the container initializes directly from the index with zero performance overhead, covering both the framework core and third-party plugins.

10. **General Bean Extension Points, User-Defined Custom Extensions**
Only the basic **BeanPostProcessor** extension point is provided; the framework does not include any built-in enhancement logic.
Capabilities such as AOP, dynamic proxy, transactions, and logging interception are all implemented by users based on this extension point—fully open, without fixed logic.

11. **Environment Configuration Loading Extension: EnvironmentLoader**
Provides a standard EnvironmentLoader extension point, supporting environment configuration loading from local and remote configuration centers, natively adapting to cloud-native configuration systems.

12. **Configuration Post-Processing Extension: EnvironmentPostProcessor**
After configuration loading is complete, custom enhancements such as encryption, decryption, masking, property replacement, and merging are supported, offering strong extensibility.

13. **Unified Specification for All Extension Points: No-Arg Constructor Instantiation**
All extension interfaces in the framework are created using a **no-arg constructor**, with controllable, clean instantiation and no dependency injection intrusion, ensuring extremely high stability.

14. **Automatic Configuration Binding, Type-Safe Mapping**
Built-in automatic configuration binding and safe type conversion, supporting multi-environment and standardized configuration management to meet enterprise-level production usage.

15. **JDK Native SPI, Plugin-Based Auto-Assembly**
Implements plugin discovery, conditional loading, and auto-assembly based on JDK native SPI mechanism, with no third-party dependencies—stable, universal, and lightweight.

16. **Graceful Shutdown, No Redundant Destruction Ordering**
Natively supports graceful shutdown without complex and meaningless Bean destruction ordering. Each object manages resource release independently—minimalist, practical, and without over-engineering.

17. **Extreme Runtime Lightweight, Minimal Memory Footprint**
After container initialization, temporary objects such as BeanDefinition, parsers, and builders are automatically destroyed.
At runtime, only **environment configuration + singleton Bean instance repository** are retained, achieving extreme memory optimization.

18. **Strict Responsibility Boundaries, Highly Pure Kernel**
The IOC kernel is only responsible for Bean management, constructor injection, and instance creation. Capabilities such as transactions, events, and monitoring are implemented through external extensions, never coupled with the kernel, ensuring long-term maintainability and evolvability of the architecture.

---

# Official Recommended Basic Configuration (Best Practice for Cloud-Native High Concurrency)
Designed for the cloud-native high-concurrency era, Pure IOC officially recommends a minimum runtime environment of **JDK 21 or above**, deeply embracing modern JVM features.

### Core Runtime Environment Recommendations
- JDK Version: **JDK 21+** (based on virtual threads, ZGC, and other modern features)
- Garbage Collector: **ZGC** (low pause, high throughput, maximizing cloud-native concurrency)
- Concurrency Model: **Virtual Threads** (lightweight concurrency, easily supporting millions of threads)

### Best Practices for Memory Configuration
Combined with Pure IOC's extremely lightweight design, the official strongly recommends small heap memory configurations instead of traditional large heap mode:
- Recommended heap memory: **2GB ~ 4GB** is fully sufficient to support high-concurrency, large-scale business scenarios.
- Design Philosophy: Small heaps combined with ZGC low-latency collection and virtual thread high-concurrency features enable millions of concurrent processing at extremely low cost, while significantly reducing Kubernetes resource scheduling pressure and runtime overhead.