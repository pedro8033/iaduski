export type Clock = () => number;

/** Mede quanto tempo (ms, arredondado) uma função assíncrona demora. O relógio é injetável para testes. */
export async function measure<T>(
  fn: () => Promise<T>,
  now: Clock = () => performance.now(),
): Promise<{ result: T; latencyMs: number }> {
  const start = now();
  const result = await fn();
  return { result, latencyMs: Math.round(now() - start) };
}
