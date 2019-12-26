/*
 * Copyright 2010-2019 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#include "Alloc.h"
#include "Atomic.h"
#include "KAssert.h"
#include "Memory.h"

namespace {

inline void lock(int* spinlock) {
  while (compareAndSwap(spinlock, 0, 1) != 0) {}
}

inline void unlock(int* spinlock) {
  RuntimeCheck(compareAndSwap(spinlock, 1, 0) == 1, "Must succeed");
}

class Locker {
  int* lock_;
 public:
  Locker(int* alock): lock_(alock) {
    lock(lock_);
  }
  ~Locker() {
    ::unlock(lock_);
  }
};

class CyclicCollector {
  int lock_;
  int currentAliveWorkers_;
 public:
  CyclicCollector() {
  }

  void addWorker(void* worker) {
    Locker lock(&lock_);
  }

  void removeWorker(void* worker) {
    Locker lock(&lock_);
  }

  void rendezvouz(void* worker) {
  }

  void garbageCollect();

  void safepointStart() {}
  void safepointEnd() {}
};

CyclicCollector* cyclicCollector = nullptr;

}  // namespace

void cyclicInit() {
  RuntimeAssert(cyclicCollector == nullptr, "Must be not yet inited");
  cyclicCollector = konanConstructInstance<CyclicCollector>();
}

void cyclicDeinit() {
  RuntimeAssert(cyclicCollector != nullptr, "Must be inited");
  konanDestructInstance(cyclicCollector);
}

void cyclicAddWorker(void* worker) {
  cyclicCollector->addWorker(worker);
}

void cyclicRemoveWorker(void* worker) {
  cyclicCollector->removeWorker(worker);
}

void cyclicRendezvouz(void* worker) {
  cyclicCollector->rendezvouz(worker);
}

void cyclicGarbageCollect() {
  cyclicCollector->garbageCollect();
}

void CyclicCollector::garbageCollect() {
  RuntimeAssert(cyclicCollector != nullptr, "Must be inited");
  ObjHolder rootsHolder;
  safepointStart();
  auto* roots = Kotlin_native_internal_GC_detectCycles(nullptr,
        rootsHolder.slot())->array();
  safepointEnd();
  if (roots == nullptr && roots->count_ == 0) return;
}
