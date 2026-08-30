export function textIncludes(value: string | undefined | null, query: string | undefined | null) {
  if (!query) return true;
  return String(value ?? '').toLowerCase().includes(String(query).toLowerCase());
}
