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
#ifndef KONAN_NO_THREADS
#define WITH_WORKERS 1
#endif

#include "Alloc.h"
#include "Atomic.h"
#include "KAssert.h"
#include "Memory.h"
#include "Natives.h"
#include "Porting.h"
#include "Types.h"

#if WITH_WORKERS
#include <pthread.h>
#endif

#if WITH_WORKERS
namespace {

class Locker {
  pthread_mutex_t* lock_;
 public:
  Locker(pthread_mutex_t* alock): lock_(alock) {
    pthread_mutex_lock(lock_);
  }
  ~Locker() {
    pthread_mutex_unlock(lock_);
  }
};

class CyclicCollector {
  pthread_mutex_t lock_;
  pthread_cond_t cond_;
  int currentAliveWorkers_;
  int currentAtRendevouz_;
  bool shallCallRendevouz_;
  bool shallCollectRoots_;
  bool shallCollectGarbage_;
  void* firstWorker_;
  ArrayHeader* roots_;
  ObjHolder rootsHolder_;
  KStdVector<int> rootsRefCounts_;

  uint32_t currentTick_;
  uint32_t lastTick_;
  uint64_t lastTimestampUs_;

  void rendezvouzHandler();

  // Here we look at the state of cyclic garbage list, and perform trial deletion using the
  // refcounts computed on the rendevouz point.
  void collectLocked() {
    konan::consolePrintf("collectLocked: %p\n", roots_);
    if (roots_ == nullptr) return;
  }

 public:
  CyclicCollector() {
     pthread_mutex_init(&lock_, nullptr);
     pthread_cond_init(&cond_, nullptr);
  }

  ~CyclicCollector() {
    pthread_cond_destroy(&cond_);
    pthread_mutex_destroy(&lock_);
  }

  void addWorker(void* worker) {
    Locker lock(&lock_);
    // We need to identify the main thread to avoid calling longer running code
    // on the first worker, as we assume is being the UI thread.
    if (firstWorker_ == nullptr) firstWorker_ = worker;
    currentAliveWorkers_++;
  }

  void removeWorker(void* worker) {
    Locker lock(&lock_);
    // When exiting the worker - we shall collect the cyclic garbage here.
    collectLocked();
    currentAliveWorkers_--;
  }

  void checkIfShallCollectLocked() {
    currentTick_++;
    auto delta = currentTick_ - lastTick_;
    if (delta > 10 || delta < 0) {
       auto currentTimestampUs = konan::getTimeMicros();
       if (currentTimestampUs - lastTimestampUs_ > 10000) {
         lastTick_ = currentTick_;
         lastTimestampUs_ = currentTimestampUs;
         shallCollectRoots_ = true;
         shallCallRendevouz_ = true;
       }
    }
  }

  void rendezvouz(void* worker) {
    Locker lock(&lock_);
    currentAtRendevouz_++;
    checkIfShallCollectLocked();
    if (!shallCallRendevouz_ && shallCollectGarbage_) {
      // TODO: maybe check worker != firstWorker_?
      collectLocked();
    }
    if (shallCallRendevouz_) {
      while (currentAtRendevouz_ < currentAliveWorkers_ && shallCallRendevouz_) {
        pthread_cond_wait(&cond_, &lock_);
      }
      if (shallCallRendevouz_) {
        rendezvouzHandler();
        shallCallRendevouz_ = false;
      }
    }
    currentAtRendevouz_--;
  }

  void scheduleGarbageCollect();
};

// Note that this code is executed on the rendevouz between all workers, and as such
// must be very short running.
void CyclicCollector::rendezvouzHandler() {
  konan::consolePrintf("at rendezvouz!!");
  if (shallCollectRoots_ && roots_ == nullptr) {
    shallCollectRoots_ = false;
    roots_ = Kotlin_native_internal_GC_detectCycles(nullptr, rootsHolder_.slot())->array();
    if (roots_ != nullptr) {
      ObjHeader** current = ArrayAddressOfElementAt(roots_, 0);
      auto count = roots_->count_;
      rootsRefCounts_.reserve(count);
      for (int i = 0; i < count; i++, current++) {
        ObjHeader* obj = *current;
        rootsRefCounts_[i] = obj->container()->refCount();
      }
      shallCollectGarbage_ = true;
    }
  }
}

void CyclicCollector::scheduleGarbageCollect() {
  Locker lock(&lock_);
  shallCallRendevouz_ = true;
  if (roots_ == nullptr)
    shallCollectRoots_ = true;
  else
    shallCollectGarbage_ = true;
}

CyclicCollector* cyclicCollector = nullptr;

}  // namespace

#endif  // WITH_WORKERS

void cyclicInit() {
#if WITH_WORKERS
  RuntimeAssert(cyclicCollector == nullptr, "Must be not yet inited");
  cyclicCollector = konanConstructInstance<CyclicCollector>();
#endif
}

void cyclicDeinit() {
#if WITH_WORKERS
  RuntimeAssert(cyclicCollector != nullptr, "Must be inited");
  konanDestructInstance(cyclicCollector);
#endif  // WITH_WORKERS
}

void cyclicAddWorker(void* worker) {
#if WITH_WORKERS
  cyclicCollector->addWorker(worker);
#endif  // WITH_WORKERS
}

void cyclicRemoveWorker(void* worker) {
#if WITH_WORKERS
  cyclicCollector->removeWorker(worker);
#endif  // WITH_WORKERS
}

void cyclicRendezvouz(void* worker) {
#if WITH_WORKERS
  cyclicCollector->rendezvouz(worker);
#endif  // WITH_WORKERS
}

void cyclicScheduleGarbageCollect() {
#if WITH_WORKERS
  cyclicCollector->scheduleGarbageCollect();
#endif  // WITH_WORKERS
}