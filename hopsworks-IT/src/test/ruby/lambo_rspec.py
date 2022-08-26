# This file is part of Hopsworks
# Copyright (C) 2022, Logical Clocks AB. All rights reserved
#
# Hopsworks is free software: you can redistribute it and/or modify it under the terms of
# the GNU Affero General Public License as published by the Free Software Foundation,
# either version 3 of the License, or (at your option) any later version.
#
# Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
# without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
# PURPOSE.  See the GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License along with this program.
# If not, see <https://www.gnu.org/licenses/>.

import os
import argparse
import subprocess
import threading
import queue
import time

from typing import List

spec_queue = queue.Queue()
output_dir = "out"
os_name = "ubuntu"
spec_done = False


def run_rspec(item: str, thread_id: int) -> None:
    env = os.environ
    env["PROC"] = str(thread_id)
    start_time = time.time()
    print("Executing {}".format(item))
    subprocess.run(
        " ".join(
            [
                "rspec",
                "--format",
                "RspecJunitFormatter",
                "--out",
                "{}_{}.xml".format(os.path.join(output_dir, item), os_name),
                "spec/{}".format(item),
                ">>",
                "/tmp/test_exec-{}".format(str(thread_id)),
                "2>&1"
            ]
        ),
        shell=True,
        env=env,
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
    )
    print("Finished {} execution in {} mins".format(item, (time.time()-start_time)/60))


def worker(thread_id: int):
    while True and not spec_done:
        item = spec_queue.get()
        run_rspec(item, thread_id)
        spec_queue.task_done()


def list_specs() -> (List[str], List[str]):
    with open("isolated_tests", "r") as f:
        isolated_tests = f.read().splitlines()

    specs = os.listdir("spec")
    return filter(specs, isolated_tests), isolated_tests


def filter(specs: List[str], isolated_test: List[str]) -> List[str]:
    return [s for s in specs if "_spec" in s and s not in isolated_test]


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Too fast too test.")
    parser.add_argument("-proc", help="The number of parallel process to use")
    parser.add_argument("-out", help="The directory where to save the tests results")
    parser.add_argument(
        "-os",
        help="The operating system on which the script is run (to name the output files)",
    )

    args = parser.parse_args()
    output_dir = args.out
    os_name = args.os
    print("Starting parallel testing with {} process".format(args.proc))

    parallel_specs, isolated_specs = list_specs()

    # Execute isolated specs
    for spec in isolated_specs:
        run_rspec(spec, 100)

    for p in range(int(args.proc)):
        threading.Thread(target=worker, daemon=True, args=(p,)).start()

    # Execute parallel specs
    for spec in parallel_specs:
        spec_queue.put(spec)

    # Wait for parallel specs to finish
    spec_queue.join()
    spec_done = True

