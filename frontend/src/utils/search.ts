import type { ProColumns } from '@ant-design/pro-components';

export function textIncludes(value: string | undefined | null, query: string | undefined | null) {
  if (!query) return true;
  return String(value ?? '').toLowerCase().includes(String(query).toLowerCase());
}

export function keywordMatches(keyword: string | undefined | null, ...values: Array<string | number | undefined | null>) {
  if (!keyword) return true;
  return values.some((value) => textIncludes(value == null ? '' : String(value), keyword));
}

export function keywordSearchColumn<T extends Record<string, any> = Record<string, any>>(
  placeholder = '编号或名称',
): ProColumns<T> {
  return {
    title: '搜索',
    dataIndex: 'keyword',
    hideInTable: true,
    fieldProps: { placeholder },
  };
}
