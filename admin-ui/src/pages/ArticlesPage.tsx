import { useState } from 'react';
import { api } from '../api';
import { Alerts, Checkbox, Field, Loading, TextArea, confirmed, formatDateTime, useAction, useLoad } from '../components/ui';
import type { Article, Tag } from '../types';

interface Editing {
  id?: number;
  title: string;
  description?: string;
  published: boolean;
  text: string;
  tagIds: number[];
}

export default function ArticlesPage() {
  const articles = useLoad(() => api.get<Article[]>('/api/admin/articles'), []);
  const tags = useLoad(() => api.get<Tag[]>('/api/admin/tags'), []);
  const [editing, setEditing] = useState<Editing>();
  const action = useAction();

  const edit = async (id?: number) => {
    if (!id) {
      setEditing({ title: '', published: false, text: '', tagIds: [] });
      return;
    }
    const article = await action.run(() => api.get<Article>(`/api/admin/articles/${id}`));
    if (article) {
      setEditing({
        id: article.id, title: article.title, description: article.description, published: article.published,
        text: article.text ?? '', tagIds: article.tags.map((t) => t.id!),
      });
    }
  };

  const save = async () => {
    if (!editing) return;
    const body = { ...editing };
    const ok = await action.run(() => editing.id ? api.put(`/api/admin/articles/${editing.id}`, body) :
      api.post('/api/admin/articles', body), 'Article saved');
    if (ok) {
      setEditing(undefined);
      articles.reload();
    }
  };

  const remove = async (a: Article) => {
    if (!confirmed(`Delete article "${a.title}"?`)) return;
    await action.run(() => api.del(`/api/admin/articles/${a.id}`), 'Article deleted');
    articles.reload();
  };

  return (
    <>
      <div className="toolbar">
        <h2>Articles</h2>
        <span className="spacer" />
        <button onClick={() => edit()}>Add article</button>
      </div>
      <Alerts error={action.error} notice={action.notice} />
      {editing && (
        <div className="panel">
          <div className="form-grid">
            <Field label="Title" value={editing.title} onChange={(v) => setEditing({ ...editing, title: v })} required
                   error={action.fieldErrors.title} />
            <Field label="Description" value={editing.description}
                   onChange={(v) => setEditing({ ...editing, description: v })} />
            <Checkbox label="Published" checked={editing.published}
                      onChange={(v) => setEditing({ ...editing, published: v })} />
            <div className="wide">
              <TextArea label="Text (HTML)" rows={14} value={editing.text}
                        onChange={(v) => setEditing({ ...editing, text: v })} />
            </div>
            <div className="wide toolbar">
              <strong>Tags:</strong>
              {tags.data?.map((t) => (
                <Checkbox key={t.id} label={t.name} checked={editing.tagIds.includes(t.id!)}
                          onChange={(on) => setEditing({
                            ...editing,
                            tagIds: on ? [...editing.tagIds, t.id!] : editing.tagIds.filter((x) => x !== t.id),
                          })} />
              ))}
            </div>
          </div>
          <div className="buttons">
            <button onClick={save} disabled={action.busy}>Save</button>
            <button className="secondary" onClick={() => setEditing(undefined)}>Cancel</button>
          </div>
          {editing.text && (
            <>
              <h4>Preview</h4>
              <div className="panel" dangerouslySetInnerHTML={{ __html: editing.text }} />
            </>
          )}
        </div>
      )}
      <Loading loading={articles.loading} error={articles.error} />
      <table>
        <thead><tr><th>Title</th><th>Tags</th><th>Published</th><th>Created</th><th /></tr></thead>
        <tbody>
        {articles.data?.map((a) => (
          <tr key={a.id}>
            <td>{a.title}</td>
            <td>{a.tags.map((t) => t.name).join(', ')}</td>
            <td>{a.published ? 'yes' : 'no'}</td>
            <td>{formatDateTime(a.createdDate)}</td>
            <td className="actions">
              <button className="secondary" onClick={() => edit(a.id)}>Edit</button>
              <button className="danger" onClick={() => remove(a)}>Delete</button>
            </td>
          </tr>
        ))}
        </tbody>
      </table>
    </>
  );
}
