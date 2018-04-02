/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package spix.swing.materialEditor.nodes;

import com.jme3.shader.*;
import spix.swing.materialEditor.*;
import spix.swing.materialEditor.controller.MatDefEditorController;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * @author Nehon
 */
public abstract class NodePanel extends DraggablePanel implements Selectable {

    protected List<JLabel> inputLabels = new ArrayList<>();
    protected List<JLabel> outputLabels = new ArrayList<>();
    protected Map<String, Dot> inputDots = new LinkedHashMap<>();
    protected Map<String, Dot> outputDots = new LinkedHashMap<>();
    private JPanel content = new JPanel();
    private JLabel header;
    protected JLabel previewLabel;
    protected Color color;
    private Icon icon;
    private String nodeName;
    private String key;
    private NodeToolBar toolBar;
    protected boolean selected = false;
    protected boolean displayPreview = false;
    protected Color backgroundColor = new Color(100, 100, 100, 200);
    protected boolean canRenameFields = false;

    GroupLayout.ParallelGroup layoutGroup1;
    GroupLayout.SequentialGroup layoutGroup2;
    JPanel inputPanel = new JPanel();
    JPanel outputPanel = new JPanel();

    private int width = 110;
    private int inputHeight, outputHeight;

    public NodePanel(MatDefEditorController controller, String key, Color color, Icon icon) {
        super(controller);
        this.color = color;
        this.icon = icon;
        this.key = key;
        toolBar = new NodeToolBar(this);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    controller.removeSelected();
                }
            }
        });
    }

    public abstract Shader.ShaderType getShaderType();

    protected abstract void initHeader(JLabel header);

    protected void init(List<ShaderNodeVariable> inputs, List<ShaderNodeVariable> outputs) {

        for (ShaderNodeVariable input : inputs) {

            JLabel label = createLabel(input.getType(), input.getName(), Dot.ParamType.Input);
            Dot dot = createDot(input.getType(), Dot.ParamType.Input, input.getName(), this.getNodeName());
            inputLabels.add(label);
            inputDots.put(this.getNodeName() + "." + input.getName(), dot);
        }
        int index = 0;
        for (ShaderNodeVariable output : outputs) {
            JLabel label = createLabel(output.getType(), output.getName(), Dot.ParamType.Output);
            Dot dot = createDot(output.getType(), Dot.ParamType.Output, output.getName(), this.getNodeName());
            dot.setIndex(index++);
            outputLabels.add(label);
            outputDots.put(this.getNodeName() + "." + output.getName(), dot);
        }

        init();
    }

    protected void init() {

        if (displayPreview) {
            width = 150;
        } else {
            width = 110;
            inputHeight = inputLabels.size() * 17;
            outputHeight = outputLabels.size() * 17;
        }
        initComponents();
        refreshBounds();
        initHeader(header);
        setOpaque(false);
    }

    private int getTotalHeight() {
        if (displayPreview) {
            return 164;
        } else {
            return 30 + inputHeight + outputHeight;
        }
    }

    private void refreshBounds() {
        setSize(outputPanel, width, outputHeight);
        setSize(inputPanel, width, inputHeight);
        if (displayPreview) {
            setSize(content, width, 134);
        } else {
            setSize(content, width, inputHeight + outputHeight);
        }
        Point loc = getLocation();
        setSize(this, width, getTotalHeight());
        setBounds(0, 0, width, getTotalHeight());
        setLocation(loc);
        revalidate();
        repaint();
    }

    public void setTitle(String s) {
        header.setText(s);
        header.setToolTipText(s);
    }

    public String getNodeName() {
        return nodeName;
    }

    protected void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public Dot getInputConnectPoint(String nameSpace, String varName) {
        return inputDots.get(nameSpace + "." + varName);
    }

    public Dot getOutputConnectPoint(String nameSpace, String varName) {
        return outputDots.get(nameSpace + "." + varName);
    }

    public Map<String, Dot> getInputConnectPoints() {
        return Collections.unmodifiableMap(inputDots);
    }

    public Map<String, Dot> getOutputConnectPoints() {
        return Collections.unmodifiableMap(outputDots);
    }

    @Override
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isSelected() {
        return selected;
    }

    @Override
    protected void paintComponent(Graphics g1) {
        Graphics2D g = (Graphics2D) g1;
        Color borderColor = Color.BLACK;
        if (selected) {
            borderColor = Color.WHITE;
        }
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, // Anti-alias!
                RenderingHints.VALUE_ANTIALIAS_ON);

        if (selected) {
            Color[] colors = new Color[]{new Color(0.6f, 0.6f, 1.0f, 0.8f), new Color(0.6f, 0.6f, 1.0f, 0.5f)};
            float[] factors = {0f, 1f};
            g.setPaint(new RadialGradientPaint(getWidth() / 2, getHeight() / 2, getWidth() / 2, factors, colors));
            g.fillRoundRect(8, 3, getWidth() - 10, getHeight() - 6, 15, 15);
        } else {
            if (toolBar.isVisible()) {
                hideToolBar();
            }
        }

        g.setColor(backgroundColor);
        g.fillRoundRect(5, 1, getWidth() - 9, getHeight() - 6, 15, 15);
        g.setColor(borderColor);

        g.drawRoundRect(4, 0, getWidth() - 9, getHeight() - 6, 15, 15);
        g.setColor(backgroundColor);
        g.fillRect(4, 1, 10, 10);
        g.setColor(borderColor);
        g.drawLine(4, 0, 14, 0);
        g.drawLine(4, 0, 4, 10);
        g.setColor(Color.BLACK);
        g.drawLine(5, 15, getWidth() - 6, 15);
        g.setColor(new Color(190, 190, 190));
        g.drawLine(5, 16, getWidth() - 6, 16);

        Color c1 = new Color(color.getRed(), color.getGreen(), color.getBlue(), 150);
        Color c2 = new Color(color.getRed(), color.getGreen(), color.getBlue(), 0);
        g.setPaint(new GradientPaint(0, 15, c1, getWidth(), 15, c2));
        g.fillRect(5, 1, getWidth() - 10, 14);

    }

    public Map<String, Dot> getInputDots() {
        return inputDots;
    }

    public Map<String, Dot> getOutputDots() {
        return outputDots;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public String getName() {
        return nodeName;
    }

    @Override
    public void onMousePressed(MouseEvent e) {
        super.onMousePressed(e);
        if (controller.select(this, e.isShiftDown() || e.isControlDown())) {
            showToolBar();
        }
    }

    @Override
    public void onMouseReleased(MouseEvent e) {
        controller.multiStopDrag();
    }

    public void showToolBar() {
        toolBar.display();
    }

    public void hideToolBar() {
        toolBar.setVisible(false);
    }

    /**
     * override to do edit this node content.
     */
    public void edit() {

    }

    public void cleanup() {
        toolBar.cleanup();
        if (toolBar.getParent() == null) {
            return;
        }
        toolBar.getParent().remove(toolBar);
    }

    private void initComponents() {
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.X_AXIS));
        headerPanel.setOpaque(false);
        setSize(headerPanel, width, 15);
        header = new JLabel(icon);
        header.setForeground(Color.white);
        header.addMouseListener(labelMouseMotionListener);
        header.addMouseMotionListener(labelMouseMotionListener);
        header.setHorizontalAlignment(SwingConstants.LEFT);
        header.setVerticalAlignment(SwingConstants.TOP);
        header.setFont(new Font("Tahoma", Font.BOLD, 9));
        header.setIconTextGap(2);
        header.setOpaque(false);
        setSize(header, 100, 15);
        headerPanel.add(makeFiller(6, 15));
        headerPanel.add(header);

        content.setOpaque(false);
        setSize(content, width, inputHeight + outputHeight);
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        add(headerPanel);
        add(makeFiller(width, 5));
        add(content);

        if (displayPreview) {
            previewLabel = new JLabel();
            previewLabel.setBackground(new java.awt.Color(100, 100, 100));
            previewLabel.setForeground(new java.awt.Color(100, 100, 100));
            previewLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            previewLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
            previewLabel.setIconTextGap(0);
            previewLabel.setMaximumSize(new java.awt.Dimension(128, 128));
            previewLabel.setMinimumSize(new java.awt.Dimension(128, 128));
            previewLabel.setOpaque(true);
            previewLabel.setPreferredSize(new java.awt.Dimension(128, 128));
            previewLabel.setBounds(11, 24, 128, 128);

            content.setLayout(new BoxLayout(content, BoxLayout.X_AXIS));
            content.add(inputDots.values().iterator().next());
            content.add(makeFillerNoAllign(1, 2));
            content.add(previewLabel);
            content.add(makeFillerNoAllign(1, 2));
            content.add(outputDots.values().iterator().next());

        } else {
            inputPanel.setOpaque(false);
            setSize(inputPanel, width, inputHeight);
            outputPanel.setOpaque(false);
            setSize(outputPanel, width, outputHeight);

            inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));
            outputPanel.setLayout(new BoxLayout(outputPanel, BoxLayout.Y_AXIS));
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

            content.add(inputPanel);
            content.add(outputPanel);
            int i = 0;
            for (String key : outputDots.keySet()) {
                addOutput(outputDots.get(key), outputLabels.get(i));
                i++;
            }

            i = 0;
            for (String key : inputDots.keySet()) {
                addInput(inputDots.get(key), inputLabels.get(i));
                i++;
            }
        }
    }


    public Box.Filler makeFiller(int width, int height) {
        Dimension d = new Dimension(width, height);
        Box.Filler filler = new Box.Filler(d, d, d);
        filler.setAlignmentY(Component.TOP_ALIGNMENT);
        filler.setAlignmentX(Component.LEFT_ALIGNMENT);
        return filler;
    }

    public Box.Filler makeFillerNoAllign(int width, int height) {
        Dimension d = new Dimension(width, height);
        Box.Filler filler = new Box.Filler(d, d, d);
        return filler;
    }

    public void setSize(JComponent vp, int width, int height) {
        vp.setMinimumSize(new Dimension(width, height));
        vp.setPreferredSize(new Dimension(width, height));
        vp.setMaximumSize(new Dimension(width, height));
        vp.setAlignmentX(Component.LEFT_ALIGNMENT);
        vp.setAlignmentY(Component.TOP_ALIGNMENT);
    }

    public void addInput(Dot dot, JLabel label) {
        JPanel p = new JPanel();
        setSize(p, width, 17);
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        p.add(dot);
        p.add(makeFillerNoAllign(2, 17));
        p.add(label);
        inputPanel.add(p);
    }

    public void insertInput(Dot dot, JLabel label) {
        inputDots.put(dot.getNodeName() + "." + dot.getVariableName(), dot);
        inputLabels.add(label);
        addInput(dot, label);
        inputHeight += 17;
        refreshBounds();
    }

    public void insertOutput(Dot dot, JLabel label) {
        outputDots.put(dot.getNodeName() + "." + dot.getVariableName(), dot);
        outputLabels.add(label);
        addOutput(dot, label);
        outputHeight += 17;
        refreshBounds();
    }

    public void addOutput(Dot dot, JLabel label) {
        JPanel p = new JPanel();

        setSize(p, width, 17);
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        p.add(Box.createHorizontalGlue());
        p.add(label);
        p.add(makeFillerNoAllign(2, 10));
        p.add(dot);
        outputPanel.add(p);

    }

    public void removeDot(Dot d) {
        Component p = d.getParent();
        Container parent = p.getParent();
        if (parent == inputPanel) {
            inputHeight -= 17;
        } else {
            outputHeight -= 17;
        }
        parent.remove(p);
        refreshBounds();
    }

    public JLabel createLabel(String glslType, String txt, Dot.ParamType type) {
        JLabel label = new JLabel(txt);
        label.setForeground(Color.WHITE);
        label.setToolTipText(glslType + " " + txt);
        label.setOpaque(false);

        label.setHorizontalAlignment(type == Dot.ParamType.Output ? SwingConstants.RIGHT : SwingConstants.LEFT);
        label.setVerticalAlignment(SwingConstants.TOP);
        label.setFont(new Font("Tahoma", 0, 9));
        label.addMouseListener(labelMouseMotionListener);
        label.addMouseMotionListener(labelMouseMotionListener);
        Dimension d = new Dimension(90, 15);
        label.setMaximumSize(d);
        label.setMinimumSize(d);
        label.setPreferredSize(d);
        return label;
    }

    public Dot createDot(String type, Dot.ParamType paramType, String paramName, String nodeName) {
        Dot dot1 = new Dot(controller);
        dot1.setShaderType(getShaderType());
        dot1.setNode(this);
        dot1.setText(paramName);
        dot1.setVariableName(paramName);
        dot1.setNodeName(nodeName);
        dot1.setParamType(paramType);
        dot1.setType(type);
        return dot1;
    }


    public final void refresh(String name) {
        setNodeName(name);
        for (Dot dot : inputDots.values()) {
            dot.setNodeName(name);
        }
        for (Dot dot : outputDots.values()) {
            dot.setNodeName(name);
        }
        setTitle(name);
    }


    public void delete() {
        controller.removeSelected();
    }

    @Override
    public String toString() {
        return nodeName;
    }

    public void renameField(JLabel source, int index, boolean isInput) {
        controller.renameNodeField(this, source, index, isInput);
    }

    public void setFieldName(String name, int index, boolean isInput) {
        if (isInput) {
            if (index >= inputLabels.size()) {
                return;
            }
            inputLabels.get(index).setText(name);
        } else {
            if (index >= outputLabels.size()) {
                return;
            }
            outputLabels.get(index).setText(name);
        }
    }

    // used to pass press and drag events to the NodePanel when they occur on the label
    private LabelMouseMotionListener labelMouseMotionListener = new LabelMouseMotionListener();

    private class LabelMouseMotionListener extends MouseAdapter {

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2 && canRenameFields) {
                // double click rename field
                JLabel source = (JLabel) e.getSource();
                int index = inputLabels.indexOf(source);
                boolean isInput = true;
                if (index == -1) {
                    index = outputLabels.indexOf(source);
                    isInput = false;
                }
                renameField(source, index, isInput);
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            MouseEvent me = SwingUtilities.convertMouseEvent(e.getComponent(), e, NodePanel.this);
            NodePanel.this.dispatchEvent(me);
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            MouseEvent me = SwingUtilities.convertMouseEvent(e.getComponent(), e, NodePanel.this);
            NodePanel.this.dispatchEvent(me);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            MouseEvent me = SwingUtilities.convertMouseEvent(e.getComponent(), e, NodePanel.this);
            NodePanel.this.dispatchEvent(me);
        }
    }
}
