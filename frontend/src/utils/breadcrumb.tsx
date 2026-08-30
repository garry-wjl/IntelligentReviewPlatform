import { Link } from 'react-router-dom';
import type { BreadcrumbProps } from 'antd';

export type CrumbItem = {
  title: string;
  path?: string;
};

export function pageBreadcrumb(items: CrumbItem[]): BreadcrumbProps {
  return {
    items: items.map((item, index) => {
      const isLast = index === items.length - 1;
      return {
        title: item.path && !isLast ? <Link to={item.path}>{item.title}</Link> : item.title,
      };
    }),
  };
}
