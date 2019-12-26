#ifndef RUNTIME_CYCLIC_COLLECTOR_H
#define RUNTIME_CYCLIC_COLLECTOR_H

void cyclicInit();
void cyclicDeinit();
void cyclicAddWorker(void* worker);
void cyclicRemoveWorker(void* worker);
void cyclicRendezvouz(void* worker);
void cyclicScheduleGarbageCollect();

#endif  // RUNTIME_CYCLIC_COLLECTOR_H