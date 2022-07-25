# mmv

Microservice Model Visualizer

## ToDos

- Microservice Types from literature (what did florian do in LEMMA?)
- How to deal with different communication patterns (messaging, event-based, CQRS?) Look for a paper
- Remake all dataclasses (Interface, Contract etc.) into normal classes because the generated toString() from data class
  messes up graph transformations, already done for Microservice::class